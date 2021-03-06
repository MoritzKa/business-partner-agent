<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-card v-if="isReady" class="mx-auto">
    <v-card-title class="bg-light">
      <v-btn depressed color="secondary" icon @click="$router.push({ name: 'Wallet' })">
        <v-icon dark>mdi-chevron-left</v-icon>
      </v-btn>
      {{ type | credentialLabel}}
      <v-layout align-end justify-end>
        <v-btn depressed color="red" icon @click="deleteDocument()">
          <v-icon dark>mdi-delete</v-icon>
        </v-btn>
      </v-layout>
    </v-card-title>
    <v-card-text>
      <Credential v-bind:document="document" ref="doc"></Credential>
      <v-divider></v-divider>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title>Public Profile</v-list-item-title>
          <v-list-item-subtitle>Visible in Public Profile</v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-switch v-model="document.isPublic"></v-switch>
        </v-list-item-action>
      </v-list-item>
      <v-divider></v-divider>
      <v-list-item v-if="this.id">
        <v-list-item-content>
          <v-list-item-title>Verification</v-list-item-title>
          <v-list-item-subtitle>Request a verification</v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn icon :to="{ name: 'RequestVerification', params: { documentId: id} }">
            <v-icon color="grey">mdi-chevron-right</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
      <v-divider></v-divider>
    </v-card-text>
    <v-card-actions></v-card-actions>
    <v-expansion-panels v-if="expertMode" accordion flat>
      <v-expansion-panel>
        <v-expansion-panel-header
          class="grey--text text--darken-2 font-weight-medium bg-light"
        >Show raw data</v-expansion-panel-header>
        <v-expansion-panel-content class="bg-light">
          <vue-json-pretty :data="document"></vue-json-pretty>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <v-card-actions>
      <v-layout align-end justify-end>
        <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
        <v-btn :loading="this.isBusy" color="primary" text @click="saveDocument()">Save</v-btn>
      </v-layout>
    </v-card-actions>
  </v-card>
</template>

<script>
import { EventBus } from "../main";
// import { CredentialTypes } from "../constants";
// import OganizationalProfile from "@/components/OrganizationalProfile";
import Credential from "@/components/Credential";
import VueJsonPretty from "vue-json-pretty";

export default {
  name: "Document",
  props: {
    id: String,
    type: String
  },
  created() {
    if (this.id) {
      EventBus.$emit("title", "Edit Document");
      this.getDocument();
    } else {
      EventBus.$emit("title", "Create new Document");
      this.document.type = this.type;
      this.isReady = true;
      this.document.isPublic = false;
    }
  },
  data: () => {
    return {
      document: {},
      isBusy: false,
      isReady: false
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    }
  },
  methods: {
    getDocument() {
      console.log(this.id);
      this.$axios
        .get(`${this.$apiBaseUrl}/wallet/document/${this.id}`)
        .then(result => {
          if ({}.hasOwnProperty.call(result, "data")) {
            this.document = result.data;
            this.isReady = true;
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    saveDocument() {
      this.isBusy = true;
      console.log(this.$refs.doc.document);
      // update document
      if (this.id) {
        this.$axios
          .put(`${this.$apiBaseUrl}/wallet/document/${this.id}`, {
            document: this.$refs.doc.documentData,
            isPublic: this.document.isPublic,
            type: this.document.type
          })
          .then(res => {
            console.log(res);
            this.isBusy = false;
            this.$router.push({
              name: "Wallet"
            });
            EventBus.$emit("success", "Success");
          })
          .catch(e => {
            this.isBusy = false;
            console.error(e);
            EventBus.$emit("error", e);
          });

        // create new document
      } else {
        this.$axios
          .post(`${this.$apiBaseUrl}/wallet/document`, {
            document: this.$refs.doc.documentData,
            isPublic: this.document.isPublic,
            type: this.type
          })
          .then(res => {
            console.log(res);
            this.isBusy = false;
            this.$router.push({
              name: "Wallet"
            });
            EventBus.$emit("success", "Success");
          })
          .catch(e => {
            this.isBusy = false;
            console.error(e);
            EventBus.$emit("error", e);
          });
      }
    },
    deleteDocument() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/wallet/document/${this.id}`)
        .then(result => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Document deleted");
            this.$router.push({
              name: "Wallet"
            });
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    cancel() {
      this.$router.push({
        name: "Wallet"
      });
    }
  },
  components: {
    // OganizationalProfile,
    Credential,
    VueJsonPretty
  }
};
</script>

<style scoped>
.bg-light {
  background-color: #fafafa;
}

.bg-light-2 {
  background-color: #ececec;
}
</style>
