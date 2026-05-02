# keycloak-twilio-verify

Keycloak browser-flow authenticator for Twilio Verify. The provider sends a Twilio Verify code to a phone number stored on the Keycloak user and validates the submitted code before the flow can continue.

The Java package and Maven group are `com.jtelaak.keycloak`.

## What it provides

- A Keycloak `AuthenticatorFactory` registered as `twilio-verify-authenticator`.
- A configurable browser-flow authenticator named `Twilio Verify`.
- A built-in login template for the verification-code challenge.
- Twilio Verify API integration using Keycloak/JDK runtime libraries, with no Twilio SDK bundled.
- Environment or system-property fallback for Twilio secrets.

## Build

```bash
mvn clean package
```

The provider jar is created at:

```text
target/keycloak-twilio-verify-1.0.0-SNAPSHOT.jar
```

Override the Keycloak API version if your server uses a different compatible version:

```bash
mvn clean package -Dkeycloak.version=25.0.4
```

## Install

Copy the jar into your Keycloak providers directory and rebuild Keycloak:

```bash
cp target/keycloak-twilio-verify-1.0.0-SNAPSHOT.jar $KEYCLOAK_HOME/providers/
$KEYCLOAK_HOME/bin/kc.sh build
$KEYCLOAK_HOME/bin/kc.sh start
```

For a container image, copy the jar to `/opt/keycloak/providers/` before running `kc.sh build`.

## Configure Twilio

Create a Twilio Verify Service and keep these values available:

- Account SID
- Auth Token
- Verify Service SID

You can enter them directly in the authenticator config, or provide them at runtime:

```bash
export TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_VERIFY_SERVICE_SID=VAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

System property fallbacks are also supported:

- `twilio.accountSid`
- `twilio.authToken`
- `twilio.verifyServiceSid`

## Configure Keycloak

1. Add an E.164 phone number to each user, for example `+15551234567`.
2. Store that value in the user attribute configured by the authenticator. The default attribute is `phone_number`.
3. In the Admin Console, open **Authentication** and copy the Browser flow.
4. Add an execution after the username/password step.
5. Choose **Twilio Verify** and set it to **Required**.
6. Open the execution config and set:
	- `Twilio Account SID`
	- `Twilio Auth Token`
	- `Verify Service SID`
	- `Phone Number User Attribute`, if you do not use `phone_number`
	- `Verification Channel`, usually `sms`
7. Bind the copied flow as the realm browser flow.

## Authenticator config

| Setting | Default | Notes |
| --- | --- | --- |
| Twilio Account SID | environment/system property fallback | Required |
| Twilio Auth Token | environment/system property fallback | Required |
| Verify Service SID | environment/system property fallback | Required |
| Phone Number User Attribute | `phone_number` | First non-blank value is used |
| Verification Channel | `sms` | Supports `sms`, `whatsapp`, or `call` |
| Max Check Attempts | `3` | Bounded from 1 to 10 |
| HTTP Timeout Seconds | `10` | Bounded from 1 to 60 |
| Twilio Verify API Base URL | `https://verify.twilio.com/v2` | Override for testing or private routing |

## Notes

- The authenticator expects phone numbers to already be normalized to E.164.
- Resending a code resets the local attempt counter for the current authentication session.
- Twilio Verify owns code generation, expiry, delivery rate limits, and final approval state.
