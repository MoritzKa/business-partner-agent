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
package org.hyperledger.oa.api;

import java.util.List;

import javax.annotation.Nullable;

import org.hyperledger.oa.impl.util.AriesStringUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * Document and credential types that the company agent can process.
 */
@Getter
@AllArgsConstructor
public enum CredentialType {
    ORGANIZATIONAL_PROFILE_CREDENTIAL(
            List.of(
                    ApiConstants.CREDENTIALS_V1,
                    "https://raw.githubusercontent.com/iil-network/contexts/master/masterdata.jsonld"),
            List.of(
                    "VerifiableCredential",
                    "OrganizationalProfileCredential"),
            "masterdata"),
    BANK_ACCOUNT_CREDENTIAL(
            List.of(
                    ApiConstants.CREDENTIALS_V1,
                    "https://raw.githubusercontent.com/iil-network/contexts/master/bankaccount.json"),
            List.of(
                    "VerifiableCredential",
                    "BankAccountCredential"),
            "bank_account"),
    OTHER(
            List.of(),
            List.of(),
            "other");

    // json-ld

    private final List<String> context;
    private final List<String> type;

    // aries credential tag

    private final String credentialTag;

    /**
     * Tries to get the type from the type list
     *
     * @param type the list of credential types
     * @return {@link CredentialType} or null when no match was found
     */
    public static @Nullable CredentialType fromType(List<String> type) {
        for (String t : type) {
            if ("OrganizationalProfileCredential".equals(t)) {
                return CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL;
            } else if ("BankAccountCredential".equals(t)) {
                return CredentialType.BANK_ACCOUNT_CREDENTIAL;
            }
        }
        return null;
    }

    /**
     * Maps the aries schema name to a {@link CredentialType}
     *
     * @param schemaId id of the schema, not the schema name
     * @return {@link CredentialType}
     */
    public static @Nullable CredentialType fromSchemaId(@NonNull String schemaId) {
        String schemaName = AriesStringUtil.schemaGetName(schemaId);
        if (ORGANIZATIONAL_PROFILE_CREDENTIAL.getCredentialTag().equals(schemaName)) {
            return ORGANIZATIONAL_PROFILE_CREDENTIAL;
        } else if (BANK_ACCOUNT_CREDENTIAL.getCredentialTag().equals(schemaName)) {
            return BANK_ACCOUNT_CREDENTIAL;
        }
        return OTHER;
    }

}
