package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
class TokenFamilyRevoker {

    private final RefreshTokenRepository refreshTokenRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(String familyId) {
        refreshTokenRepository.revokeAllByFamilyId(familyId);
    }
}