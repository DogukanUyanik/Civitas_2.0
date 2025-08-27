package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public List<Member> getMembers() {
        return memberRepository.findAll();
    }
}
