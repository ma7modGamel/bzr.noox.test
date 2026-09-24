# خط الواجهة

- `Cairo-Regular.ttf` / `Cairo-Medium.ttf` / `Cairo-SemiBold.ttf` / `Cairo-Bold.ttf`: ملف ثابت لكل وزن مستخدم (43 §2)، وهي اللي بتتضمّن في التطبيقين والويب عن طريق `tools/gen-design`.
- `Cairo-Variable.ttf`: الملف المتغيّر الأصلي (Google Fonts، OFL)، ومنه اتولّدت الملفات الثابتة. مش بيتضمّن في التطبيقات، لأن تطبيق أوزان الخط المتغيّر مش مضمون بنفس الطريقة في Compose وSwiftUI.
- `OFL.txt`: الرخصة.

إعادة توليد الأوزان (بعد تغيير الخط، OD-07):

```bash
pip install fonttools
for w in 400:Regular 500:Medium 600:SemiBold 700:Bold; do
  fonttools varLib.instancer Cairo-Variable.ttf wght=${w%%:*} slnt=0 --update-name-table -o Cairo-${w##*:}.ttf
done
```

لو اتغيّر اسم الخط أو الأوزان، يتحدّث `tokens.json → font` في نفس التغيير.
