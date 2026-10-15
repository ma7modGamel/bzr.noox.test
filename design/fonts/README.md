# خطوط التصميم

## خط الواجهة: IBM Plex Sans Arabic (DEC-070)

- `IBMPlexSansArabic-Regular.ttf` / `-Medium` / `-SemiBold` / `-Bold`: ملف ثابت لكل وزن مستخدم (43 §2)، من Google Fonts (`ofl/ibmplexsansarabic`، إصدار 1.101). هي اللي بتتضمّن في التطبيقين والويب عن طريق `tools/gen-design`.
- `OFL.txt`: رخصة IBM Plex، وبتتنسخ لتطبيق iOS.
- مصدر الاختيار: ملف Figma «Primo App». المالك اعتمده في 2026-10-09، وده قفل OD-07.

لو اتغيّر اسم الخط أو الأوزان، لازم `tokens.json → font` يتحدّث في نفس التغيير.

## خط الشعار: Cairo

- `Cairo-*.ttf` و`Cairo-Variable.ttf`: الشعار النصي بس (`tools/gen-brand-assets`). مش بيتضمّن في التطبيقات.
- `OFL-Cairo.txt`: رخصة Cairo.
