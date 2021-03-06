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
package org.hyperledger.oa.impl.activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.aries.api.credential.Credential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableCredentialBuilder;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation.VerifiablePresentationBuilder;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.aries.config.TimeUtil;
import org.hyperledger.oa.api.ApiConstants;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.aries.BankAccount;
import org.hyperledger.oa.api.aries.BankAccountVC;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.DidDocWeb;
import org.hyperledger.oa.model.MyCredential;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.repository.DidDocWebRepository;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.hyperledger.oa.repository.MyDocumentRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Async;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;

@Singleton
public class VPManager {

    @Inject
    private Identity id;

    @Inject
    private MyDocumentRepository docRepo;

    @Inject
    private MyCredentialRepository credRepo;

    @Inject
    private DidDocWebRepository didRepo;

    @Inject
    private PartnerRepository partnerRepo;

    @Inject
    private CryptoManager crypto;

    @Inject
    @Setter(AccessLevel.PROTECTED)
    private Converter converter;

    public void recreateVerifiablePresentation() {
        List<VerifiableCredential> vcs = new ArrayList<>();

        String myDid = id.getMyDid();

        docRepo.findByIsPublicTrue().forEach(doc -> {
            vcs.add(buildFromDocument(doc, myDid));
        });
        credRepo.findByIsPublicTrue().forEach(cred -> {
            vcs.add(buildFromCredential(cred, myDid));
        });

        // only split up into own method, because of a weird issue that the second
        // thread does
        // not see the newly created document otherwise.
        signVP(vcs);
    }

    @Async(value = TaskExecutors.SCHEDULED)
    public void signVP(List<VerifiableCredential> vcs) {
        final VerifiablePresentationBuilder vpBuilder = VerifiablePresentation.builder();
        if (vcs.size() > 0) {
            vpBuilder.verifiableCredential(vcs);
        } else {
            vpBuilder.verifiableCredential(null);
        }
        Optional<VerifiablePresentation> vp = crypto.sign(vpBuilder.build());
        getVerifiablePresentationInternal().ifPresentOrElse(didWeb -> {
            didRepo.updateProfileJson(didWeb.getId(), converter.toMap(vp));
        }, () -> {
            didRepo.save(DidDocWeb
                    .builder()
                    .profileJson(converter.toMap(vp))
                    .build());
        });
    }

    protected VerifiableCredential buildFromDocument(@NonNull MyDocument doc, @NonNull String myDid) {
        Object subj;
        if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(doc.getType())) {
            BankAccount ba = converter.fromMap(doc.getDocument(), BankAccount.class);
            subj = new BankAccountVC(myDid, ba);
        } else {
            final ObjectNode on = converter.fromMap(doc.getDocument(), ObjectNode.class);
            on.remove("id");
            on.put("id", myDid);
            // this is needed because the java client serializes with GSON
            // and cannot handle Jackson ObjectNode
            subj = GsonConfig.defaultConfig().fromJson(on.toString(), Object.class);
        }
        return VerifiableCredential
                .builder()
                .id("urn:" + doc.getId().toString())
                .type(doc.getType().getType())
                .context(doc.getType().getContext())
                .issuanceDate(TimeUtil.currentTimeFormatted())
                .issuer(myDid)
                .credentialSubject(subj)
                .build();
    }

    private VerifiableCredential buildFromCredential(@NonNull MyCredential cred, @NonNull String myDid) {
        final ArrayList<String> type = new ArrayList<>(cred.getType().getType());
        type.add("IndyCredential");

        final ArrayList<String> context = new ArrayList<>(cred.getType().getContext());
        context.add(ApiConstants.INDY_CREDENTIAL_SCHEMA);

        Credential ariesCred = converter.fromMap(cred.getCredential(), Credential.class);

        Object credSubj;
        if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(cred.getType())) {
            BankAccount ba = ariesCred.to(BankAccount.class);
            credSubj = new BankAccountVC(myDid, ba);
        } else {
            credSubj = converter.fromMap(cred.getCredential(), Object.class);
        }
        VerifiableCredentialBuilder builder = VerifiableCredential.builder()
                .id("urn:" + cred.getId().toString())
                .type(type)
                .context(context)
                .issuanceDate(TimeUtil.currentTimeFormatted(cred.getIssuedAt()))
                .schemaId(ariesCred.getSchemaId())
                .credDefId(ariesCred.getCredentialDefinitionId())
                .credentialSubject(credSubj);
        partnerRepo.findByConnectionId(cred.getConnectionId()).ifPresent(p -> builder.indyIssuer(p.getDid()));
        return builder.build();
    }

    public Optional<VerifiablePresentation> getVerifiablePresentation() {
        final Optional<DidDocWeb> dbVP = getVerifiablePresentationInternal();
        if (dbVP.isPresent() && dbVP.get().getProfileJson() != null) {
            return Optional.of(converter.fromMap(dbVP.get().getProfileJson(), VerifiablePresentation.class));
        }
        return Optional.empty();
    }

    private Optional<DidDocWeb> getVerifiablePresentationInternal() {
        Optional<DidDocWeb> result = Optional.empty();
        final Iterator<DidDocWeb> iterator = didRepo.findAll().iterator();
        if (iterator.hasNext()) {
            result = Optional.of(iterator.next());
            if (iterator.hasNext()) {
                throw new IllegalStateException("More than one did doc entity found");
            }
        }
        return result;
    }
}
