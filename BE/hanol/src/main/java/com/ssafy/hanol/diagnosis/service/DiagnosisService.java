package com.ssafy.hanol.diagnosis.service;

import com.ssafy.hanol.common.exception.CustomException;
import com.ssafy.hanol.common.util.s3.ImageUploadUtil;
import com.ssafy.hanol.diagnosis.controller.DiagnosisIdListApiResponse;
import com.ssafy.hanol.diagnosis.controller.dto.request.DiagnosisSendApiRequest;
import com.ssafy.hanol.diagnosis.controller.dto.response.DiagnosisDetailApiResponse;
import com.ssafy.hanol.diagnosis.domain.Diagnosis;
import com.ssafy.hanol.diagnosis.exception.DiagnoseErrorCode;
import com.ssafy.hanol.diagnosis.repository.DiagnosisRepository;
import com.ssafy.hanol.diagnosis.service.dto.request.DiagnosisAiRequest;
import com.ssafy.hanol.diagnosis.service.dto.request.DiagnosisRequest;
import com.ssafy.hanol.diagnosis.service.dto.response.DiagnosisListResponse;
import com.ssafy.hanol.diagnosis.service.dto.response.RabbitmqResponse;
import com.ssafy.hanol.diagnosis.service.rabbitmq.DiagnosisRequestProducer;
import com.ssafy.hanol.global.sse.service.SseService;
import com.ssafy.hanol.global.sse.service.dto.response.DiagnoseAiResultResponse;
import com.ssafy.hanol.member.domain.Member;
import com.ssafy.hanol.member.exception.MemberErrorCode;
import com.ssafy.hanol.member.repository.MemberRepository;
import com.ssafy.hanol.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final ImageUploadUtil imageUploadUtil;
    private final DiagnosisRequestProducer diagnosisRequestProducer;
    private final SseService sseService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    public DiagnosisDetailApiResponse findDiagnosis(Long diagnosisId, Long memberId) {
        Diagnosis diagnosis = null;

        // 최신 데이터 조회
        if (diagnosisId == 0) {
            diagnosis = diagnosisRepository.findTopByMemberIdOrderByIdDesc(memberId).orElse(null);
            if (diagnosis == null) return null; // 진단이 존재하지 않으면 null 반환

        } else { // 특정 id의 데이터 조회
            diagnosis = diagnosisRepository.findById(diagnosisId)
                    .orElseThrow(() -> new CustomException(DiagnoseErrorCode.NOT_FOUND_DIAGNOSIS));
            validateValue(diagnosis);
            validateAccessRights(diagnosis, memberId);
        }

        return DiagnosisDetailApiResponse.from(DiagnosisInfo.from(diagnosis));
    }


    public DiagnosisListResponse findDiagnoses(Integer limit, Long memberId) {
        Boolean applyLimit = false;
        if (limit != null) {
            applyLimit = true;
        }

        List<DiagnosisInfo> diagnosisInfoList = diagnosisRepository.findDiagnoses(memberId, applyLimit, limit);
        return DiagnosisListResponse.builder()
                .diagnosisInfoList(diagnosisInfoList)
                .build();
    }

    // TODO: 진단하기 -> AI 서버 통신(RabbitMQ) -> 이벤트 등록
    //  진단 결과 listen -> 이미지 업로드, 데이터 저장 -> 결과 return
    public void diagnose(DiagnosisRequest diagnosisRequest) {

        Member member = memberService.findMemberByMemberId(diagnosisRequest.getMemberId());

        // 이미지 저장
        String imageUrl = imageUploadUtil.uploadImage("test", diagnosisRequest.getFile(), member.getId());
        log.info("imageUrl : {}", imageUrl);

        Diagnosis diagnosis = Diagnosis.builder()
                .member(member)
                .scanPart(diagnosisRequest.getScanPart())
                .deviceType(diagnosisRequest.getDeviceType())
                .imageUrl(imageUrl)
                .value1(-1)
                .value2(-1)
                .value3(-1)
                .value4(-1)
                .value5(-1)
                .value6(-1)
                .build();
        log.info(diagnosis.toString());

        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);

        // AI 서버로 이미지 진단 요청 전송
        DiagnosisAiRequest diagnosisAiRequest = null;
        try {
            diagnosisAiRequest = DiagnosisAiRequest.from(savedDiagnosis.getId(), member.getId(), diagnosisRequest.getFile());
            diagnosisRequestProducer.sendDiagnosisRequest(diagnosisAiRequest);
        } catch (Exception e) {
            throw new CustomException(DiagnoseErrorCode.FILE_CONVERSION_ERROR);
        }
    }

    public void saveDiagnosisAndSend(RabbitmqResponse rabbitmqResponse) {
        Diagnosis diagnosis = diagnosisRepository.findById(rabbitmqResponse.getKeyId()).orElseThrow();

        // 진단 결과 저장
        diagnosis.updateValues(rabbitmqResponse);
        log.info(diagnosis.toString());

        sseService.sendDiagnosisResult(rabbitmqResponse.getSseId(), DiagnoseAiResultResponse.from(diagnosis));
    }


    public DiagnosisIdListApiResponse findDiagnosisIds(Long memberId) {
        List<DiagnosisIdInfo> diagnosisIdList = diagnosisRepository.findDiagnosisIds(memberId);
        return DiagnosisIdListApiResponse.from(diagnosisIdList);
    }


    // 다른 회원에게 진단 결과 전송하기
    public void sendDiagnosisResult(Long diagnosisId, DiagnosisSendApiRequest request) {
        // 받는 회원 존재 여부, 전송할 진단 존재 여부 체크
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(MemberErrorCode.NOT_FOUND_MEMBER));
        Diagnosis currentDiagnosis = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new CustomException(DiagnoseErrorCode.NOT_FOUND_DIAGNOSIS));

        Diagnosis newDiagnosis = new Diagnosis(currentDiagnosis, member);
        diagnosisRepository.save(newDiagnosis);
    }


    // 본인의 진단 결과인지 검사
    private void validateAccessRights(Diagnosis diagnosis, Long memberId) {
        if (!diagnosis.getMember().getId().equals(memberId)) {
            throw new CustomException(DiagnoseErrorCode.FORBIDDEN_ACCESS);
        }
    }

    // AI 진단 결과가 없는 데이터
    private static void validateValue(Diagnosis diagnosis) {
        if(diagnosis.getValue1() == -1) {
            throw new CustomException(DiagnoseErrorCode.EMPTY_VALUE);
        }
    }

}
