<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.push({ name: 'Partners' })">
          <v-icon dark>mdi-chevron-left</v-icon>
        </v-btn>
        <span v-if="!isUpdatingName">{{ partner.name }}</span>
        <v-text-field
          class="mt-8"
          v-else
          label="Name"
          append-icon="mdi-done"
          v-model="alias"
          outlined
          dense
        >
          <template v-slot:append>
            <v-btn class="pb-1" text @click="isUpdatingName=false">Cancel</v-btn>
            <v-btn
              class="pb-1"
              text
              color="primary"
              :loading="isBusy"
              @click="submitNameUpdate()"
            >Save</v-btn>
          </template>
        </v-text-field>
        <PartnerStateIndicator v-if="partner.state" v-bind:state="partner.state"></PartnerStateIndicator>
        <v-layout align-end justify-end>
          <v-btn if="depressed" icon @click="isUpdatingName = !isUpdatingName">
            <v-icon dark>mdi-pencil</v-icon>
          </v-btn>
          <v-btn depressed color="primary" icon @click="refreshPartner()">
            <v-icon dark>mdi-refresh</v-icon>
          </v-btn>

          <v-btn depressed color="red" icon @click="deletePartner()">
            <v-icon dark>mdi-delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>

      <v-card-text>
        <OganizationalProfile v-if="partner.profile" v-bind:document="partner.profile" isReadOnly></OganizationalProfile>
        <v-row class="mx-4">
          <v-col cols="4">
            <v-row>
              <p class="grey--text text--darken-2 font-weight-medium">Received Presentations</p>
            </v-row>
            <v-row>The presentations you received from your partner</v-row>
            <v-row v-if="expertMode" class="mt-4">
              <v-btn
                small
                :to="{ name: 'RequestPresentation', params: { id: id }  }"
              >Request Presentation</v-btn>
            </v-row>
          </v-col>
          <v-col cols="8">
            <v-card flat>
              <CredentialList v-if="isReady" v-bind:credentials="credentials"></CredentialList>
            </v-card>
          </v-col>
        </v-row>
        <v-row class="mx-4">
          <v-divider></v-divider>
        </v-row>
        <v-row class="mx-4">
          <v-col cols="4">
            <v-row>
              <p class="grey--text text--darken-2 font-weight-medium">Shared Credentials</p>
            </v-row>
            <v-row>The credentials you share with your partner</v-row>
            <v-row v-if="expertMode" class="mt-4">
              <v-btn small :to="{ name: 'SendPresentation', params: { id: id }  }">Send Presentation</v-btn>
            </v-row>
          </v-col>
          <v-col cols="8">
            <CredentialList v-if="isReady" v-bind:credentials="[]" v-bind:headers="headersSent"></CredentialList>
          </v-col>
        </v-row>
      </v-card-text>

      <v-card-actions>
        <v-expansion-panels v-if="expertMode" accordion flat>
          <v-expansion-panel>
            <v-expansion-panel-header
              class="grey--text text--darken-2 font-weight-medium bg-light"
            >Show raw data</v-expansion-panel-header>
            <v-expansion-panel-content class="bg-light">
              <vue-json-pretty :data="rawData"></vue-json-pretty>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import VueJsonPretty from "vue-json-pretty";
import OganizationalProfile from "@/components/OrganizationalProfile";
import CredentialList from "@/components/CredentialList";
import PartnerStateIndicator from "@/components/PartnerStateIndicator";
import { CredentialTypes } from "../constants";
import { getPartnerProfile, getPartnerName } from "../utils/partnerUtils";
import { EventBus } from "../main";
export default {
  name: "Partner",
  props: ["id"],
  components: {
    VueJsonPretty,
    OganizationalProfile,
    CredentialList,
    PartnerStateIndicator
  },
  created() {
    this.getPartner();
  },
  data: () => {
    return {
      isReady: false,
      isBusy: false,
      isUpdatingName: false,
      alias: "",
      partner: {},
      rawData: {},
      credentials: [],
      headersSent: [
        {
          text: "Type",
          value: "type"
        },
        {
          text: "Issuer",
          value: "issuer"
        },
        {
          text: "Sent at",
          value: "sentAt"
        }
      ]
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    }
  },
  methods: {
    getPartner() {
      console.log("Getting partner...");
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/${this.id}`)
        .then(result => {
          console.log(result);
          if ({}.hasOwnProperty.call(result, "data")) {
            this.rawData = result.data;
            this.partner = {
              ...result.data,
              ...{
                profile: getPartnerProfile(result.data)
              }
            };
            if ({}.hasOwnProperty.call(this.partner, "credential")) {
              // Show only creds other than OrgProfile in credential list
              this.credentials = this.partner.credential.filter(cred => {
                return cred.type !== CredentialTypes.PROFILE.name;
              });
            }

            // Hacky way to define a partner name
            // Todo: Make this consistent. Probalby in backend
            this.partner.name = getPartnerName(this.partner);
            this.alias = this.partner.name;
            this.isReady = true;
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    deletePartner() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/partners/${this.id}`)
        .then(result => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Partner deleted");
            this.$router.push({
              name: "Partners"
            });
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    refreshPartner() {
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/${this.id}/refresh`)
        .then(result => {
          if (result.status === 200) {
            EventBus.$emit("success", "Partner updated");
            if ({}.hasOwnProperty.call(result, "data")) {
              this.rawData = result.data;
              this.partner = {
                ...result.data,
                ...{
                  profile: getPartnerProfile(result.data)
                }
              };
            }
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    submitNameUpdate() {
      this.isBusy = true;
      this.$axios
        .put(`${this.$apiBaseUrl}/partners/${this.id}`, {
          alias: this.alias
        })
        .then(result => {
          if (result.status === 200) {
            this.isBusy = false;
            this.partner.name = this.alias;
            this.isUpdatingName = false;
          }
        })
        .catch(e => {
          this.isBusy = false;
          this.isUpdatingName = false;
          console.error(e);
          EventBus.$emit("error", e);
        });
    }
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
