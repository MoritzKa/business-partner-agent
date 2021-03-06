/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.aries;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.proof.PresentationExchangeRecord;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.impl.util.AriesStringUtil;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.model.PartnerProof;
import org.hyperledger.oa.repository.PartnerProofRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.scheduling.annotation.Async;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiresAries
public class ConnectionManager {

    @Value("${oagent.did.prefix}")
    private String didPrefix;

    @Inject
    private AriesClient ac;

    @Inject
    private PartnerRepository partnerRepo;

    @Inject
    private PartnerProofRepository partnerPrepo;

    @Async
    public void createConnection(@NonNull String did, @NonNull String label, @Nullable String alias) {
        try {
            ac.connectionsReceiveInvitation(
                    ReceiveInvitationRequest.builder()
                            .did(AriesStringUtil.didGetLastSegment(did))
                            .label(label)
                            .build(),
                    alias);
        } catch (IOException e) {
            log.error("Could not create aries connection", e);
        }
    }

    public synchronized void handleConnectionEvent(ConnectionRecord connection) {
        partnerRepo.findByLabel(connection.getTheirLabel()).ifPresentOrElse(dbP -> {
            if (dbP.getConnectionId() == null) {
                dbP.setConnectionId(connection.getConnectionId());
                dbP.setState(connection.getState());
                partnerRepo.update(dbP);
            } else {
                partnerRepo.updateState(dbP.getId(), connection.getState());
            }
        }, () -> {
            // new incoming connetion
            Partner p = Partner
                    .builder()
                    .ariesSupport(Boolean.TRUE)
                    .alias(connection.getTheirLabel())
                    .connectionId(connection.getConnectionId())
                    .did(didPrefix + connection.getTheirDid())
                    .label(connection.getTheirLabel())
                    .state(connection.getState())
                    .incoming(Boolean.TRUE)
                    .build();
            partnerRepo.save(p);
        });
    }

    public boolean removeConnection(String connectionId) {
        log.debug("Removing connection: {}", connectionId);
        try {
            ac.connectionsRemove(connectionId);

            partnerRepo.findByConnectionId(connectionId).ifPresent(p -> {
                final List<PartnerProof> proofs = partnerPrepo.findByPartnerId(p.getId());
                if (CollectionUtils.isNotEmpty(proofs)) {
                    partnerPrepo.deleteAll(proofs);
                }
            });

            ac.presentProofRecords().ifPresent(records -> {
                final List<String> toDelete = records.stream()
                        .filter(r -> r.getConnectionId().equals(connectionId))
                        .map(PresentationExchangeRecord::getPresentationExchangeId)
                        .collect(Collectors.toList());
                toDelete.forEach(presExId -> {
                    try {
                        ac.presentProofRecordsRemove(presExId);
                    } catch (IOException e) {
                        log.error("Could not delete presentation exchange record: {}", presExId, e);
                    }
                });
            });
        } catch (IOException e) {
            log.error("Could not delete connection: {}", connectionId, e);
        }

        return false;
    }
}
