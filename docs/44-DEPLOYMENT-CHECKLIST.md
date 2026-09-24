# 44 — قائمة تحقق النشر

هذه القائمة إلزامية لكل نشر إلى `staging` أو `production`، وتكمل 30 و32.

## الخصوصية والأمان

- [ ] **DEP-PRIV-01:** إعداد خادم الويب الفعلي يعطّل access log لكل `/v/*`، بما في ذلك query string، قبل تمرير الطلب إلى Laravel:

  ```nginx
  location ^~ /v/ {
      access_log off;
      try_files $uri /index.php?$query_string;
  }
  ```

- [ ] إعداد موازن الحمل أو CDN، إن وُجد، لا يسجل path أو query string لمسارات `/v/*`.
- [ ] اختبار staging يفتح رابط مشاركة صالحًا ومنتهيًا، ثم يؤكد من سجلات Nginx والموازن أن رمز المشاركة لم يظهر.
- [ ] `Cache-Control: no-store` و`X-Robots-Tag: noindex, nofollow` موجودان في استجابة رابط المشاركة.

يفحص `tools/check-deployment-docs` وجود إعداد Nginx أعلاه في الوثائق. التحقق من **الإعداد الفعلي والسجلات** يبقى خطوة تشغيل يدوية لأن مستودع التطبيق لا يملك إعداد الخادم المنشور.

## الهوية والنطاق (DEC-048)

- [ ] **DEP-ID-01:** `APP_DOMAIN` مضبوط على نطاق المنتج (`bremo` + الامتداد المعتمد)، و`APP_URL="https://${APP_DOMAIN}"`. لا يُكتب نطاق في الكود؛ عناوين البريد والروابط العامة تُشتق منه.
- [ ] `APP_NAME="بريمو - Bremo"`. التطبيقان: `applicationId = com.bremo.app` و`bundle id = com.bremo.app`، والاسم تحت الأيقونة «بريمو».
- [ ] الصفحات العامة تعمل على النطاق: `/terms` و`/privacy` و`/cancellation-policy` (DEC-051)، وصفحات W02 (`/email/verify/…` و`/password/reset/…`).

## البريد: ZeptoMail وZoho Mail (DEC-049)

البريد التلقائي (التفعيل، استعادة كلمة المرور، إشعارات NTF ذات البريد) يُرسل من `no-reply@{APP_DOMAIN}` عبر **ZeptoMail**، وصندوق الدعم `support@{APP_DOMAIN}` على **Zoho Mail**.

1. **Zoho Mail (صندوق الدعم):** إضافة النطاق في Zoho Mail، ثم التحقق من ملكيته بسجل `TXT` الذي تعطيه Zoho، ثم إنشاء صندوق `support@{APP_DOMAIN}`.
2. **سجلات MX (استقبال بريد Zoho):** `mx.zoho.com` (أولوية 10)، `mx2.zoho.com` (20)، `mx3.zoho.com` (50) — أو نسخة المنطقة التي تعرضها لوحة Zoho للحساب.
3. **ZeptoMail (الإرسال التلقائي):** إنشاء Mail Agent، وإضافة النطاق فيه، ونسخ سجلات التحقق التي يعرضها.
4. **SPF:** سجل `TXT` واحد فقط على جذر النطاق يجمع المُرسلَين:
   `v=spf1 include:zoho.com include:zeptomail.net ~all`
   (يُستبدل بالقيمة الدقيقة التي تعرضها لوحتا Zoho وZeptoMail للحساب إن اختلفت؛ لا يُضاف سجل SPF ثانٍ).
5. **DKIM:** سجلا `TXT` منفصلان بالمحدِّدين اللذين تعطيهما Zoho Mail وZeptoMail (مثل `zmail._domainkey` و`…._domainkey`)، ثم زر «تحقق» في كل لوحة حتى يظهر مفعّلًا.
6. **DMARC:** سجل `TXT` على `_dmarc.{APP_DOMAIN}`، يبدأ بالمراقبة:
   `v=DMARC1; p=none; rua=mailto:support@{APP_DOMAIN}; adkim=s; aspf=s`
   وبعد أسبوعين من تقارير سليمة يُرفع إلى `p=quarantine`.
7. **الإعداد في env:**
   - عبر API (مفضل): `MAIL_MAILER=zeptomail`، `ZEPTOMAIL_API_KEY=<Send Mail Token>`، و`ZEPTOMAIL_API_URL` حسب منطقة الحساب (الافتراضي `https://api.zeptomail.com/v1.1/email`).
   - أو عبر SMTP: `MAIL_MAILER=smtp`، `MAIL_HOST=smtp.zeptomail.com`، `MAIL_PORT=587`، `MAIL_USERNAME=emailapikey`، `MAIL_PASSWORD=<Send Mail Token>`.
   - `MAIL_FROM_ADDRESS` و`MAIL_SUPPORT_ADDRESS` لا يُضبطان إلا إذا اختلفا عن `no-reply@` و`support@` على `APP_DOMAIN`.
- [ ] **DEP-MAIL-01:** اختبار إرسال حقيقي من الإنتاج: تسجيل حساب ببريد خارجي (Gmail وOutlook) ← وصول رسالة التفعيل في الوارد (لا الرسائل غير المرغوب فيها)، بالعربية وRTL واسم بريمو، والرابط يفتح W02 ويفعّل الحساب.
- [ ] **DEP-MAIL-02:** «نسيت كلمة المرور» ← وصول الرسالة ← صفحة W02 تعيّن كلمة المرور وتُخرج كل الأجهزة.
- [ ] **DEP-MAIL-03:** رسالة من «المساعدة والدعم» (C29) تصل إلى `support@{APP_DOMAIN}` في Zoho Mail.
- [ ] **DEP-MAIL-04:** رؤوس رسالة مستلمة تُظهر `spf=pass` و`dkim=pass` و`dmarc=pass`.
- [ ] `php artisan queue:work` يعمل على الخادم؛ رسائل البريد تُرسل من الطابور.

## الدفع (DEC-050)

- [ ] المدير العام أدخل عنوان إنستاباي والاسم الظاهر (CFG-056/057) قبل تفعيل القناة؛ القناة لا تظهر في التطبيق بدون العنوان.
- [ ] CFG-055 (فوري) يبقى معطّلًا حتى بيانات حساب فوري (OD-10)، و`FAWRY_DRIVER` لا يساوي `staging` في الإنتاج.
