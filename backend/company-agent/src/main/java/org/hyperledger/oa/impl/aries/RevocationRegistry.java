package org.hyperledger.oa.impl.aries;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.creddef.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.creddef.CredentialDefinition.CredentialDefinitionResponse;
import org.hyperledger.aries.api.creddef.CredentialDefinition.CredentialDefinitionsCreated;
import org.hyperledger.aries.api.creddef.CredentialDefinitionFilter;
import org.hyperledger.aries.api.revocation.RevRegCreateRequest;
import org.hyperledger.aries.api.revocation.RevRegCreateResponse;
import org.hyperledger.aries.api.revocation.RevRegUpdateTailsFileUri;
import org.hyperledger.aries.api.revocation.RevRegsCreated;
import org.hyperledger.aries.api.revocation.RevocationRegistryState;
import org.hyperledger.oa.config.runtime.RequiresAries;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * To be able to issue credentials a revocation registry needs to be created for
 * each credential definition
 *
 * https://hyperledger-indy.readthedocs.io/projects/sdk/en/latest/docs/concepts/revocation/cred-revocation.html
 */
@Slf4j
@Singleton
@RequiresAries
public class RevocationRegistry {

    @Inject
    @Setter(value = AccessLevel.PACKAGE)
    private AriesClient ac;

    @Value("${oagent.host}")
    @Setter(value = AccessLevel.PACKAGE)
    private String host;

    public void createRevRegForCredDefIfNeeded(@NonNull String credDefId) {
        try {
            final Optional<RevRegsCreated> activeRevReg = ac.revocationRegistriesCreated(
                    credDefId, RevocationRegistryState.active);
            if (activeRevReg.isEmpty() || CollectionUtils.isEmpty(activeRevReg.get().getRevRegIds())) {
                final Optional<RevRegCreateResponse> revReg = ac.revocationCreateRegistry(RevRegCreateRequest
                        .builder()
                        .credentialDefinitionId(credDefId)
                        .build());
                if (revReg.isPresent()) {
                    String revRegId = revReg.get().getRevocRegId();
                    final Optional<RevRegCreateResponse> uriUpdate = ac.revocationRegistryUpdateUri(
                            revRegId, new RevRegUpdateTailsFileUri(
                                    host + "/revocation/registry/" + revRegId + "/tails-file"));
                    if (uriUpdate.isPresent()) {
                        log.info("Tails file public URI: {}", uriUpdate.get().getTailsPublicUri());
                        final Optional<RevRegCreateResponse> activeReg = ac.revocationRegistryPublish(revRegId);
                        if (activeReg.isPresent()) {
                            log.info("Revocation registry for cred def id: {} is now active", credDefId);
                        }
                    }
                }
            } else {
                log.info("Found active revocation registry: {}", activeRevReg.get().getRevRegIds());
            }
        } catch (IOException e) {
            log.error("Aries Exception: ", e);
        }
    }

    Optional<String> getOrCreateCredDefForSchema(@NonNull String schemaId, @NonNull String tag) {
        Optional<String> credDefId = Optional.empty();
        try {
            final Optional<CredentialDefinitionsCreated> credDef = ac.credentialDefinitionsCreated(
                    CredentialDefinitionFilter
                            .builder()
                            .schemaId(schemaId)
                            .build());
            if (credDef.isPresent()
                    && CollectionUtils.isNotEmpty(credDef.get().getCredentialDefinitionIds())) {
                credDefId = Optional.of(credDef.get().getCredentialDefinitionIds().get(0));
                log.info("Found existing credential definition id: {}", credDefId.get());
            } else {
                final Optional<CredentialDefinitionResponse> credDefResp = ac
                        .credentialDefinitionsCreate(CredentialDefinitionRequest
                                .builder()
                                .schemaId(schemaId)
                                .supportRevocation(Boolean.TRUE)
                                .tag(tag)
                                .build());
                if (credDefResp.isPresent()) {
                    credDefId = Optional.of(credDefResp.get().getCredentialDefinitionId());
                    log.info("Created new credential definition: {}", credDefId);
                } else {
                    log.warn("Could not create credential definition.");
                }
            }
        } catch (IOException e) {
            log.error("Aries Exception: ", e);
        }
        return credDefId;
    }
}
