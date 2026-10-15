# أيقونات الفئات (OD-08)

رموز Material Symbols Outlined بلون واحد زي ملف Figma «Primo App» (DEC-070)، بمساحة `viewBox="0 0 48 48"` ومسار واحد مملوء بـ `TONE_A`.
اللونان رمزان يستبدلهما `tools/gen-design` من `tokens.json → categoryIcon`:

- `TONE_A`: اللون الأساسي.
- `TONE_B`: الخلفية الفاتحة.

المخرج: `public/design/categories/<key>.svg`، ويعيده الخادم في `categories.icon_url` فتتبدل الأيقونة بلا تحديث للتطبيق.
الفئة المفعّلة يلزم أن يكون لها أيقونة (DEC-053).
