import os

translations = {
    'values': {
        "label_relationship_you": "You",
        "reminder_water_notes": "Hydration accelerates tissue repair and skin barrier restoration.",
        "reminder_check_temperature_title": "Check temperature",
        "reminder_check_temperature_notes": "Monitor temperature for early fever or infection warning.",
        "reminder_observe_area_title": "Skin Care & Observation",
        "reminder_observe_area_notes": "Check for any changes in redness, size, or sensation."
    },
    'values-vi': {
        "label_relationship_you": "Bạn",
        "reminder_water_notes": "Uống đủ nước giúp đẩy nhanh quá trình phục hồi mô và hàng rào bảo vệ da.",
        "reminder_check_temperature_title": "Kiểm tra nhiệt độ",
        "reminder_check_temperature_notes": "Theo dõi nhiệt độ để phát hiện sớm dấu hiệu sốt hoặc nhiễm trùng.",
        "reminder_observe_area_title": "Chăm sóc & Quan sát da",
        "reminder_observe_area_notes": "Kiểm tra xem có bất kỳ thay đổi nào về độ đỏ, kích thước hoặc cảm giác không."
    },
    'values-es': {
        "label_relationship_you": "Tú",
        "reminder_water_notes": "La hidratación acelera la reparación de los tejidos y la barrera cutánea.",
        "reminder_check_temperature_title": "Controlar la temperatura",
        "reminder_check_temperature_notes": "Controle la temperatura para detectar signos tempranos de fiebre o infección.",
        "reminder_observe_area_title": "Cuidado y observación de la piel",
        "reminder_observe_area_notes": "Verifique si hay cambios en el enrojecimiento, tamaño o sensación."
    },
    'values-fr': {
        "label_relationship_you": "Vous",
        "reminder_water_notes": "L\\'hydratation accélère la réparation des tissus et de la barrière cutanée.",
        "reminder_check_temperature_title": "Vérifier la température",
        "reminder_check_temperature_notes": "Surveillez la température pour détecter rapidement de la fièvre ou une infection.",
        "reminder_observe_area_title": "Soins et observation de la peau",
        "reminder_observe_area_notes": "Vérifiez tout changement de rougeur, de taille ou de sensation."
    },
    'values-de': {
        "label_relationship_you": "Du",
        "reminder_water_notes": "Die Flüssigkeitszufuhr beschleunigt die Gewebereparatur und die Wiederherstellung der Hautbarriere.",
        "reminder_check_temperature_title": "Temperatur messen",
        "reminder_check_temperature_notes": "Überwachen Sie die Temperatur zur Früherkennung von Fieber oder Infektionen.",
        "reminder_observe_area_title": "Hautpflege & Beobachtung",
        "reminder_observe_area_notes": "Achten Sie auf Veränderungen von Rötung, Größe oder Missempfindungen."
    },
    'values-pt-rBR': {
        "label_relationship_you": "Você",
        "reminder_water_notes": "A hidratação acelera a reparação tecidual e a restauração da barreira cutânea.",
        "reminder_check_temperature_title": "Verificar temperatura",
        "reminder_check_temperature_notes": "Monitore a temperatura para detectar precocemente febre ou infecção.",
        "reminder_observe_area_title": "Cuidados e observação da pele",
        "reminder_observe_area_notes": "Verifique quaisquer alterações na vermelhidão, tamanho ou sensação."
    },
    'values-pt-rPT': {
        "label_relationship_you": "Você",
        "reminder_water_notes": "A hidratação acelera a reparação tecidual e a restauração da barreira cutânea.",
        "reminder_check_temperature_title": "Verificar temperatura",
        "reminder_check_temperature_notes": "Monitore a temperatura para detetar precocemente febre ou infeção.",
        "reminder_observe_area_title": "Cuidados e observação da pele",
        "reminder_observe_area_notes": "Verifique quaisquer alterações na vermelhidão, tamanho ou sensação."
    },
    'values-ja': {
        "label_relationship_you": "あなた",
        "reminder_water_notes": "水分補給により組織の修復と皮膚バリアの回復が促進されます。",
        "reminder_check_temperature_title": "体温測定",
        "reminder_check_temperature_notes": "発熱や感染の初期兆候を早期に検知するために体温を記録します。",
        "reminder_observe_area_title": "スキンケアと経過観察",
        "reminder_observe_area_notes": "赤み、大きさ、感覚の変化がないか確認してください。"
    },
    'values-ko': {
        "label_relationship_you": "나",
        "reminder_water_notes": "수분 공급은 조직 재생과 피부 장벽 회복을 촉진합니다.",
        "reminder_check_temperature_title": "체온 측정",
        "reminder_check_temperature_notes": "발열 또는 감염 조기 경고를 위해 체온을 모니터링하세요.",
        "reminder_observe_area_title": "피부 관리 및 관찰",
        "reminder_observe_area_notes": "발적, 크기 또는 감각의 변화가 있는지 확인하세요."
    },
    'values-zh-rCN': {
        "label_relationship_you": "本人",
        "reminder_water_notes": "充足的水分可加速组织修复并恢复皮肤屏障。",
        "reminder_check_temperature_title": "测量体温",
        "reminder_check_temperature_notes": "监测体温以及早发现发烧或感染迹象。",
        "reminder_observe_area_title": "皮肤护理与观察",
        "reminder_observe_area_notes": "观察红肿、大小或感觉是否有任何变化。"
    },
    'values-zh-rTW': {
        "label_relationship_you": "本人",
        "reminder_water_notes": "充足的水分可加速組織修復並恢復皮膚屏障。",
        "reminder_check_temperature_title": "測量體溫",
        "reminder_check_temperature_notes": "監測體溫以及早發現發燒或感染跡象。",
        "reminder_observe_area_title": "皮膚護理與觀察",
        "reminder_observe_area_notes": "觀察紅腫、大小或感覺是否有任何變化。"
    },
    'values-ru': {
        "label_relationship_you": "Вы",
        "reminder_water_notes": "Гидратация ускоряет восстановление тканей и кожного барьера.",
        "reminder_check_temperature_title": "Измерить температуру",
        "reminder_check_temperature_notes": "Следите за температурой для раннего выявления признаков лихорадки или инфекции.",
        "reminder_observe_area_title": "Уход за кожей и наблюдение",
        "reminder_observe_area_notes": "Проверьте, нет ли изменений в покраснении, размере или ощущениях."
    },
    'values-hi': {
        "label_relationship_you": "आप",
        "reminder_water_notes": "जलयोजन ऊतक की मरम्मत और त्वचा की बाधा को बहाल करने में तेजी लाता है।",
        "reminder_check_temperature_title": "तापमान जांचें",
        "reminder_check_temperature_notes": "बुखार या संक्रमण की शुरुआती चेतावनी के लिए तापमान की निगरानी करें।",
        "reminder_observe_area_title": "त्वचा की देखभाल और अवलोकन",
        "reminder_observe_area_notes": "लालिमा, आकार या संवेदना में किसी भी बदलाव की जांच करें।"
    },
    'values-in': {
        "label_relationship_you": "Anda",
        "reminder_water_notes": "Hidrasi mempercepat perbaikan jaringan dan pemulihan pelindung kulit.",
        "reminder_check_temperature_title": "Periksa suhu tubuh",
        "reminder_check_temperature_notes": "Pantau suhu tubuh untuk deteksi dini demam atau tanda infeksi.",
        "reminder_observe_area_title": "Perawatan & Pengamatan Kulit",
        "reminder_observe_area_notes": "Periksa apakah ada perubahan pada kemerahan, ukuran, atau sensasi."
    },
    'values-id': {
        "label_relationship_you": "Anda",
        "reminder_water_notes": "Hidrasi mempercepat perbaikan jaringan dan pemulihan pelindung kulit.",
        "reminder_check_temperature_title": "Periksa suhu tubuh",
        "reminder_check_temperature_notes": "Pantau suhu tubuh untuk deteksi dini demam atau tanda infeksi.",
        "reminder_observe_area_title": "Perawatan & Pengamatan Kulit",
        "reminder_observe_area_notes": "Periksa apakah ada perubahan pada kemerahan, ukuran, atau sensasi."
    },
    'values-ar': {
        "label_relationship_you": "أنت",
        "reminder_water_notes": "الترطيب يسرع إصلاح الأنسجة واستعادة حاجز الجلد.",
        "reminder_check_temperature_title": "قياس درجة الحرارة",
        "reminder_check_temperature_notes": "راقب درجة الحرارة للكشف المبكر عن الحمى أو العدوى.",
        "reminder_observe_area_title": "العناية بالبشرة والمراقبة",
        "reminder_observe_area_notes": "تحقق من أي تغييرات في الاحمرار أو الحجم أو الإحساس."
    },
    'values-tr': {
        "label_relationship_you": "Sen",
        "reminder_water_notes": "Hidrasyon, doku onarımını ve cilt bariyerinin yenilenmesini hızlandırır.",
        "reminder_check_temperature_title": "Ateşi ölç",
        "reminder_check_temperature_notes": "Erken ateş veya enfeksiyon belirtilerini izlemek için ateşi kontrol edin.",
        "reminder_observe_area_title": "Cilt Bakımı ve Gözlem",
        "reminder_observe_area_notes": "Kızarıklık, boyut veya hisste herhangi bir değişiklik olup olmadığını kontrol edin."
    },
    'values-bn': {
        "label_relationship_you": "আপনি",
        "reminder_water_notes": "পর্যাপ্ত জল পান টিস্যু মেরামত ও ত্বকের সুরক্ষা স্তর পুনরুদ্ধার দ্রুত করে।",
        "reminder_check_temperature_title": "তাপমাত্রা পরীক্ষা করুন",
        "reminder_check_temperature_notes": "জ্বর বা সংক্রমণের প্রাথমিক সতর্কতার জন্য তাপমাত্রা পর্যবেক্ষণ করুন।",
        "reminder_observe_area_title": "ত্বকের যত্ন ও পর্যবেক্ষণ",
        "reminder_observe_area_notes": "লালভাব, আকার বা অনুভূতিতে কোনো পরিবর্তন আছে কিনা তা পরীক্ষা করুন।"
    }
}

base_res = "app/src/main/res"

for folder, keys in translations.items():
    file_path = os.path.join(base_res, folder, "strings.xml")
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        continue
    
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    new_entries = []
    for k, v in keys.items():
        if f'name="{k}"' not in content:
            new_entries.append(f'    <string name="{k}">{v}</string>')
    
    if new_entries:
        insert_text = "\n" + "\n".join(new_entries) + "\n</resources>"
        content = content[:-12] + insert_text if content.rstrip().endswith("</resources>") else content.replace("</resources>", insert_text)
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"Updated {folder} with {len(new_entries)} keys.")
    else:
        print(f"{folder} already has all keys.")

