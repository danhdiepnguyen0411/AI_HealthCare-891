import os, re

translations = {
    'values': {
        "report_harmful_content": "Harmful content",
        "report_illegal_content": "Illegal content",
        "report_offensive_content": "Offensive content",
        "report_biased_content": "Biased content",
        "report_trademark_violation": "Trademark violation",
        "report_copyright_violation": "Copyright violation",
        "report_nudity_sexual_content": "Nudity/sexual content",
        "report_violence_gore": "Violence/gore",
        "report_others": "Others",
        "toast_report_submitted": "Thank you for reporting. We will review this result."
    },
    'values-vi': {
        "report_harmful_content": "Nội dung có hại",
        "report_illegal_content": "Nội dung bất hợp pháp",
        "report_offensive_content": "Nội dung gây khó chịu",
        "report_biased_content": "Nội dung sai lệch, thiên vị",
        "report_trademark_violation": "Vi phạm nhãn hiệu",
        "report_copyright_violation": "Vi phạm bản quyền",
        "report_nudity_sexual_content": "Nội dung khỏa thân/tình dục",
        "report_violence_gore": "Bạo lực/máu me",
        "report_others": "Khác",
        "toast_report_submitted": "Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét kết quả này."
    },
    'values-es': {
        "report_harmful_content": "Contenido dañino",
        "report_illegal_content": "Contenido ilegal",
        "report_offensive_content": "Contenido ofensivo",
        "report_biased_content": "Contenido sesgado",
        "report_trademark_violation": "Infracción de marca registrada",
        "report_copyright_violation": "Infracción de derechos de autor",
        "report_nudity_sexual_content": "Desnudez/contenido sexual",
        "report_violence_gore": "Violencia/sangre",
        "report_others": "Otros",
        "toast_report_submitted": "Gracias por informar. Revisaremos este resultado."
    },
    'values-fr': {
        "report_harmful_content": "Contenu préjudiciable",
        "report_illegal_content": "Contenu illégal",
        "report_offensive_content": "Contenu offensant",
        "report_biased_content": "Contenu biaisé",
        "report_trademark_violation": "Violation de marque déposée",
        "report_copyright_violation": "Violation du droit d\\'auteur",
        "report_nudity_sexual_content": "Nudité/contenu sexuel",
        "report_violence_gore": "Violence/gore",
        "report_others": "Autres",
        "toast_report_submitted": "Merci pour votre signalement. Nous allons examiner ce résultat."
    },
    'values-de': {
        "report_harmful_content": "Schädlicher Inhalt",
        "report_illegal_content": "Illegaler Inhalt",
        "report_offensive_content": "Anstößiger Inhalt",
        "report_biased_content": "Voreingenommener Inhalt",
        "report_trademark_violation": "Markenrechtsverletzung",
        "report_copyright_violation": "Urheberrechtsverletzung",
        "report_nudity_sexual_content": "Nacktheit/sexuelle Inhalte",
        "report_violence_gore": "Gewalt/Gore",
        "report_others": "Sonstiges",
        "toast_report_submitted": "Vielen Dank für Ihre Meldung. Wir werden dieses Ergebnis überprüfen."
    },
    'values-pt-rBR': {
        "report_harmful_content": "Conteúdo prejudicial",
        "report_illegal_content": "Conteúdo ilegal",
        "report_offensive_content": "Conteúdo ofensivo",
        "report_biased_content": "Conteúdo tendencioso",
        "report_trademark_violation": "Violação de marca registrada",
        "report_copyright_violation": "Violação de direitos autorais",
        "report_nudity_sexual_content": "Nudez/conteúdo sexual",
        "report_violence_gore": "Violência/conteúdo explícito",
        "report_others": "Outros",
        "toast_report_submitted": "Obrigado por relatar. Analisaremos este resultado."
    },
    'values-pt-rPT': {
        "report_harmful_content": "Conteúdo prejudicial",
        "report_illegal_content": "Conteúdo ilegal",
        "report_offensive_content": "Conteúdo ofensivo",
        "report_biased_content": "Conteúdo tendencioso",
        "report_trademark_violation": "Violação de marca registrada",
        "report_copyright_violation": "Violação de direitos autorais",
        "report_nudity_sexual_content": "Nudez/conteúdo sexual",
        "report_violence_gore": "Violência/conteúdo explícito",
        "report_others": "Outros",
        "toast_report_submitted": "Obrigado por relatar. Analisaremos este resultado."
    },
    'values-ja': {
        "report_harmful_content": "有害なコンテンツ",
        "report_illegal_content": "違法なコンテンツ",
        "report_offensive_content": "不快なコンテンツ",
        "report_biased_content": "偏ったコンテンツ",
        "report_trademark_violation": "商標権侵害",
        "report_copyright_violation": "著作権侵害",
        "report_nudity_sexual_content": "ヌード/性的コンテンツ",
        "report_violence_gore": "暴力/残酷な表現",
        "report_others": "その他",
        "toast_report_submitted": "ご報告ありがとうございます。内容を確認いたします。"
    },
    'values-ko': {
        "report_harmful_content": "유해한 콘텐츠",
        "report_illegal_content": "불법 콘텐츠",
        "report_offensive_content": "불쾌한 콘텐츠",
        "report_biased_content": "편향된 콘텐츠",
        "report_trademark_violation": "상표권 침해",
        "report_copyright_violation": "저작권 침해",
        "report_nudity_sexual_content": "누드/성적 콘텐츠",
        "report_violence_gore": "폭력/잔혹한 표현",
        "report_others": "기타",
        "toast_report_submitted": "신고해 주셔서 감사합니다. 검토하도록 하겠습니다."
    },
    'values-zh-rCN': {
        "report_harmful_content": "有害内容",
        "report_illegal_content": "违法内容",
        "report_offensive_content": "冒犯性内容",
        "report_biased_content": "带有偏见的内容",
        "report_trademark_violation": "侵犯商标权",
        "report_copyright_violation": "侵犯版权",
        "report_nudity_sexual_content": "裸露/色情内容",
        "report_violence_gore": "暴力/血腥",
        "report_others": "其他",
        "toast_report_submitted": "感谢您的举报，我们将尽快审核此结果。"
    },
    'values-zh-rTW': {
        "report_harmful_content": "有害內容",
        "report_illegal_content": "違法內容",
        "report_offensive_content": "冒犯性內容",
        "report_biased_content": "帶有偏見的內容",
        "report_trademark_violation": "侵犯商標權",
        "report_copyright_violation": "侵犯版權",
        "report_nudity_sexual_content": "裸露/色情內容",
        "report_violence_gore": "暴力/血腥",
        "report_others": "其他",
        "toast_report_submitted": "感謝您的舉報，我們將儘快審核此結果。"
    },
    'values-ru': {
        "report_harmful_content": "Вредоносный контент",
        "report_illegal_content": "Незаконный контент",
        "report_offensive_content": "Оскорбительный контент",
        "report_biased_content": "Предвзятый контент",
        "report_trademark_violation": "Нарушение товарного знака",
        "report_copyright_violation": "Нарушение авторских прав",
        "report_nudity_sexual_content": "Нагота/сексуальный контент",
        "report_violence_gore": "Насилие/жестокость",
        "report_others": "Другое",
        "toast_report_submitted": "Спасибо за сообщение. Мы проверим этот результат."
    },
    'values-hi': {
        "report_harmful_content": "हानिकारक सामग्री",
        "report_illegal_content": "अवैध सामग्री",
        "report_offensive_content": "आपत्तिजनक सामग्री",
        "report_biased_content": "पक्षपातपूर्ण सामग्री",
        "report_trademark_violation": "ट्रेडमार्क का उल्लंघन",
        "report_copyright_violation": "कॉपीराइट का उल्लंघन",
        "report_nudity_sexual_content": "नग्नता/यौन सामग्री",
        "report_violence_gore": "हिंसा/खून-खराबा",
        "report_others": "अन्य",
        "toast_report_submitted": "रिपोर्ट करने के लिए धन्यवाद। हम इस परिणाम की समीक्षा करेंगे।"
    },
    'values-in': {
        "report_harmful_content": "Konten berbahaya",
        "report_illegal_content": "Konten ilegal",
        "report_offensive_content": "Konten menyinggung",
        "report_biased_content": "Konten bias",
        "report_trademark_violation": "Pelanggaran merek dagang",
        "report_copyright_violation": "Pelanggaran hak cipta",
        "report_nudity_sexual_content": "Ketelanjangan/konten seksual",
        "report_violence_gore": "Kekerasan/kesadisan",
        "report_others": "Lainnya",
        "toast_report_submitted": "Terima kasih atas laporan Anda. Kami akan meninjau hasil ini."
    },
    'values-id': {
        "report_harmful_content": "Konten berbahaya",
        "report_illegal_content": "Konten ilegal",
        "report_offensive_content": "Konten menyinggung",
        "report_biased_content": "Konten bias",
        "report_trademark_violation": "Pelanggaran merek dagang",
        "report_copyright_violation": "Pelanggaran hak cipta",
        "report_nudity_sexual_content": "Ketelanjangan/konten seksual",
        "report_violence_gore": "Kekerasan/kesadisan",
        "report_others": "Lainnya",
        "toast_report_submitted": "Terima kasih atas laporan Anda. Kami akan meninjau hasil ini."
    },
    'values-ar': {
        "report_harmful_content": "محتوى ضار",
        "report_illegal_content": "محتوى غير قانوني",
        "report_offensive_content": "محتوى مسيء",
        "report_biased_content": "محتوى متحيز",
        "report_trademark_violation": "انتهاك العلامة التجارية",
        "report_copyright_violation": "انتهاك حقوق النشر",
        "report_nudity_sexual_content": "عري/محتوى جنسي",
        "report_violence_gore": "عنف/مشاهد دموية",
        "report_others": "أخرى",
        "toast_report_submitted": "شكراً لإبلاغنا. سنقوم بمراجعة هذه النتيجة."
    },
    'values-tr': {
        "report_harmful_content": "Zararlı içerik",
        "report_illegal_content": "Yasa dışı içerik",
        "report_offensive_content": "Saldırgan içerik",
        "report_biased_content": "Taraflı içerik",
        "report_trademark_violation": "Ticari marka ihlali",
        "report_copyright_violation": "Telif hakkı ihlali",
        "report_nudity_sexual_content": "Çıplaklık/cinsel içerik",
        "report_violence_gore": "Şiddet/vahşet",
        "report_others": "Diğer",
        "toast_report_submitted": "Bildirdiğiniz için teşekkür ederiz. Bu sonucu inceleyeceğiz."
    },
    'values-bn': {
        "report_harmful_content": "ক্ষতিকর বিষয়বস্তু",
        "report_illegal_content": "অবৈধ বিষয়বস্তু",
        "report_offensive_content": "আপত্তিকর বিষয়বস্তু",
        "report_biased_content": "পক্ষপাতমূলক বিষয়বস্তু",
        "report_trademark_violation": "ট্রেডমার্ক লঙ্ঘন",
        "report_copyright_violation": "কপিরাইট লঙ্ঘন",
        "report_nudity_sexual_content": "নগ্নতা/যৌন বিষয়বস্তু",
        "report_violence_gore": "সহিংসতা/রক্তাক্ত দৃশ্য",
        "report_others": "অন্যান্য",
        "toast_report_submitted": "রিপোর্ট করার জন্য ধন্যবাদ। আমরা এই ফলাফলটি পর্যালোচনা করব।"
    }
}

base_dir = 'app/src/main/res'
for folder, string_dict in translations.items():
    file_path = os.path.join(base_dir, folder, 'strings.xml')
    if not os.path.exists(file_path):
        continue
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    to_add = []
    for k, v in string_dict.items():
        if f'name="{k}"' not in content:
            to_add.append(f'    <string name="{k}">{v}</string>\n')

    if to_add:
        idx = content.rfind('</resources>')
        if idx != -1:
            new_content = content[:idx] + '    <!-- Report Result Reasons -->\n' + ''.join(to_add) + content[idx:]
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated {file_path} with {len(to_add)} report keys")

