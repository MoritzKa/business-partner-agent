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
package org.hyperledger.oa.impl.util;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.oa.BaseTest;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.MyDocumentAPI;
import org.hyperledger.oa.api.PartnerAPI;
import org.hyperledger.oa.impl.CredentialTestUtils;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.model.Partner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.test.annotation.MicronautTest;

@MicronautTest
class ConverterTest extends BaseTest {

    @Inject
    private Converter conv;

    private CredentialTestUtils utils;

    @BeforeEach
    public void setupUtils() {
        utils = new CredentialTestUtils(mapper);
    }

    @Test
    void testConvertWalletDocumentRequest() throws Exception {
        final String content = "{\"test\":\"test\"}";
        final JsonNode node = mapper.readValue(content, JsonNode.class);

        final Map<String, Object> map = conv.toMap(node);
        final JsonNode fromMap = conv.fromMap(map, JsonNode.class);

        String contentResult = mapper.writeValueAsString(fromMap);
        assertEquals(content, contentResult);
    }

    @Test
    void testConvertVPToPartnerApi() throws Exception {
        VerifiablePresentation vp = loadAndConvertTo("files/verifiablePresentation.json", VerifiablePresentation.class);
        final PartnerAPI partner = conv.toAPIObject(vp);
        assertEquals(2, partner.getCredential().size());
        assertEquals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, partner.getCredential().get(0).getType());
        assertEquals(CredentialType.BANK_ACCOUNT_CREDENTIAL, partner.getCredential().get(1).getType());
    }

    @Test
    void testConvertVPToPartnerModel() throws Exception {
        VerifiablePresentation vp = loadAndConvertTo("files/verifiablePresentation.json", VerifiablePresentation.class);
        final PartnerAPI partner = conv.toAPIObject(vp);
        final Partner model = conv.toModelObject("did:web:test.foo", partner);
        assertTrue(model.getDid().startsWith("did"));
        assertEquals(vp, conv.fromMap(model.getVerifiablePresentation(), VerifiablePresentation.class));
    }

    @Test
    void testConvertCredentialToModelObject() throws Exception {
        MyDocumentAPI c = utils.createDummyCred(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, Boolean.TRUE);
        c.setCreatedDate(Long.valueOf(123l)); // should not be used but set by database layer
        c.setId(UUID.randomUUID()); // should not used but set by database layer

        MyDocument result = conv.toModelObject(c);

        assertEquals(c.getType(), result.getType());
        assertEquals(c.getIsPublic(), result.getIsPublic());
        assertTrue(result.getDocument().containsValue("Hello"));
        assertEquals(c.getIsPublic(), result.getIsPublic());
        assertNotEquals(c.getCreatedDate(), result.getCreatedAt());
        assertNotEquals(c.getId(), result.getId());
    }

}
