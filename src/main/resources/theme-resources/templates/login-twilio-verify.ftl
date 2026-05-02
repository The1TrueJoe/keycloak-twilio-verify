<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${msg("twilioVerifyTitle")}
    <#elseif section = "form">
        <form id="kc-twilio-verify-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <#if maskedPhoneNumber?? && maskedPhoneNumber?has_content>
                <div class="${properties.kcFormGroupClass!}">
                    <p class="${properties.kcInfoAreaWrapperClass!}">${msg("twilioVerifyPrompt", maskedPhoneNumber)}</p>
                </div>
                <div class="${properties.kcFormGroupClass!}">
                    <label for="code" class="${properties.kcLabelClass!}">${msg("twilioVerifyCodeLabel")}</label>
                    <input id="code"
                           name="code"
                           type="text"
                           class="${properties.kcInputClass!}"
                           inputmode="numeric"
                           autocomplete="one-time-code"
                           autofocus />
                </div>
                <#if remainingAttempts??>
                    <div class="${properties.kcFormGroupClass!}">
                        <span class="${properties.kcInputHelperTextClass!}">${msg("twilioVerifyAttemptsRemaining", remainingAttempts)}</span>
                    </div>
                </#if>
                <div class="${properties.kcFormGroupClass!}">
                    <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               type="submit"
                               value="${msg("doSubmit")}" />
                        <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                name="resend"
                                value="true"
                                type="submit">${msg("twilioVerifyResend")}</button>
                    </div>
                </div>
            <#else>
                <div class="${properties.kcFormGroupClass!}">
                    <p class="${properties.kcInfoAreaWrapperClass!}">${msg("twilioVerifyCannotContinue")}</p>
                </div>
            </#if>
        </form>
    </#if>
</@layout.registrationLayout>
