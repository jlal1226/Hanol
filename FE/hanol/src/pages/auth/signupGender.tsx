import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MaleIcon from '../../assets/images/male_icon.png';
import FemaleIcon from '../../assets/images/female_icon.png';
import DisabledButton from 'components/button/DisabledButton';
import TapBarDepth2 from './../../components/common/TapBarDepth2';
import { GenderInfo } from 'recoil/atoms';
import { useSetRecoilState, useRecoilValue } from 'recoil';
import { BirthInfo } from 'recoil/atoms';
import axiosInstance from 'api/axiosInterceptor';
import { MemberRoleState } from 'recoil/atoms';

function SignupGender() {
  const buttonName = '확인';
  const navigate = useNavigate();

  const [gender, setGender] = useState<string | null>(null); // 초기값은 null 또는 문자열
  const setGenderInfo = useSetRecoilState(GenderInfo);
  const birth = useRecoilValue(BirthInfo);
  const Gender = useRecoilValue(GenderInfo);
  const setMemberRole = useSetRecoilState(MemberRoleState);

  const handleManClick = () => {
    setGender('man');
    setGenderInfo('MALE');
  };

  const handleWomanClick = () => {
    setGender('woman');
    setGenderInfo('FEMALE');
  };

  const handleButtonClick = async () => {
    try {
      const requsetBody = { birth: birth, gender: Gender };
      const response = await axiosInstance.post('/members/signup', requsetBody);
      const access_token = response.data.data.access_token;
      localStorage.setItem('access_token', access_token);
    } catch (error) {
      console.error(error);
    }
    navigate('/login-done');
    setMemberRole('USER');
  };

  const handleCloseClick = () => {
    navigate('/login');
  };
  return (
    <div className="col-span-full h-screen flex flex-col justify-between">
      <div>
        <TapBarDepth2 name={'회원가입'} onClick={handleCloseClick} propsIsBack={true} rightBtnType={2} />
        <div className="flex">
          <div className="h-[0.188rem] w-[67%] bg-Main  left-0"></div>
          <div className="h-[0.188rem] w-[33%] bg-Gray  left-[67%]"></div>
        </div>
      </div>
      <div>
        <div className="font-bold text-[18px] text-black flex">성별은</div>
        <div className="font-bold text-[18px] text-black flex">어떻게 되시나요?</div>
        <div className="mt-[12px] font-regular text-[12px] text-GrayForText flex">
          성별에 따라 제공되는 솔루션이 달라집니다.
        </div>
      </div>
      <div className="flex justify-center items-center">
        <div
          className={`w-[10rem] mr-[1rem] rounded-[1rem] ${gender === 'man' ? 'shadow-lg' : 'shadow'}`}
          onClick={handleManClick}
        >
          <img src={MaleIcon} />
          <div className="font-medium text-[1rem] mb-[0.5rem]">남성</div>
        </div>
        <div
          className={`w-[10rem] rounded-[1rem] ${gender === 'woman' ? 'shadow-lg' : 'shadow'}`}
          onClick={handleWomanClick}
        >
          <img src={FemaleIcon} />
          <div className="font-medium text-[1rem] mb-[0.5rem]">여성</div>
        </div>
      </div>
      <div className="mb-[4rem]">
        <div className="flex justify-center mb-[1rem]">
          <div className="font-regular text-[12px] text-GrayForText items-center underline">서비스 이용약관</div>
          <div className="font-regular text-[12px] text-GrayForText items-center">,</div>
          <div className="font-regular text-[12px] text-GrayForText items-center underline">개인정보 처리방침</div>
          <div className="font-regular text-[12px] text-GrayForText items-center">에 동의합니다.</div>
        </div>
        <DisabledButton name={buttonName} onClick={handleButtonClick} disabled={gender === null} />
      </div>
    </div>
  );
}

export default SignupGender;
