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
package org.hyperledger.oa.api.aries;

import java.util.Map;
import java.util.UUID;

import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.model.MyCredential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AriesCredential {
    private UUID id;
    private Long issuedAt;
    private CredentialType type;
    private String state;
    private Boolean isPublic;

    private String issuer;
    private String schemaId;
    private Map<String, String> credentialData;

    public static class AriesCredentialBuilder {
    } // javadoc plugin cannot handle lombok builder

    public static AriesCredentialBuilder fromMyCredential(@NonNull MyCredential c) {
        return AriesCredential
                .builder()
                .id(c.getId())
                .issuedAt(c.getIssuedAt() != null ? Long.valueOf(c.getIssuedAt().toEpochMilli()) : null)
                .type(c.getType())
                .state(c.getState())
                .isPublic(c.getIsPublic());
    }
}
