(ns piyodash.solid-foods
  (:require [clojure.string :as str]))

(def ingredient-catalog
  [{:id :rice :ja "おかゆ・米" :en "Rice / porridge" :group :staples
    :patterns [#"(?<!パン)(?:10倍がゆ|十倍がゆ|7倍がゆ|お粥|おかゆ)"]}
   {:id :wheat :ja "パン・小麦" :en "Bread / wheat" :group :staples
    :patterns [#"パン(?:がゆ)?" #"小麦"]}
   {:id :udon :ja "うどん" :en "Udon" :group :staples :patterns [#"うどん"]}
   {:id :somen :ja "そうめん" :en "Somen" :group :staples :patterns [#"そうめん|素麺"]}
   {:id :oats :ja "オートミール" :en "Oatmeal" :group :staples :patterns [#"オートミール|燕麦"]}
   {:id :pasta :ja "パスタ" :en "Pasta" :group :staples :patterns [#"パスタ|マカロニ|スパゲッティ"]}

   {:id :pumpkin :ja "かぼちゃ" :en "Pumpkin" :group :vegetables :patterns [#"かぼちゃ|カボチャ"]}
   {:id :carrot :ja "にんじん" :en "Carrot" :group :vegetables :patterns [#"にんじん|人参"]}
   {:id :spinach :ja "ほうれん草" :en "Spinach" :group :vegetables :patterns [#"ほうれん草"]}
   {:id :komatsuna :ja "小松菜" :en "Komatsuna" :group :vegetables :patterns [#"小松菜"]}
   {:id :broccoli :ja "ブロッコリー" :en "Broccoli" :group :vegetables :patterns [#"ブロッコリー"]}
   {:id :tomato :ja "トマト" :en "Tomato" :group :vegetables :patterns [#"トマト"]}
   {:id :onion :ja "たまねぎ" :en "Onion" :group :vegetables :patterns [#"たまねぎ|玉ねぎ"]}
   {:id :potato :ja "じゃがいも" :en "Potato" :group :vegetables :patterns [#"じゃがいも|ポテト"]}
   {:id :sweet-potato :ja "さつまいも" :en "Sweet potato" :group :vegetables :patterns [#"さつまいも|薩摩芋"]}
   {:id :turnip :ja "かぶ" :en "Turnip" :group :vegetables :patterns [#"かぶ|カブ"]}
   {:id :daikon :ja "だいこん" :en "Daikon" :group :vegetables :patterns [#"だいこん|大根"]}
   {:id :cabbage :ja "キャベツ" :en "Cabbage" :group :vegetables :patterns [#"キャベツ"]}
   {:id :napa :ja "白菜" :en "Napa cabbage" :group :vegetables :patterns [#"白菜"]}
   {:id :corn :ja "とうもろこし" :en "Corn" :group :vegetables :patterns [#"とうもろこし|コーン"]}
   {:id :cauliflower :ja "カリフラワー" :en "Cauliflower" :group :vegetables :patterns [#"カリフラワー"]}
   {:id :peas :ja "グリーンピース" :en "Green peas" :group :vegetables :patterns [#"グリーンピース|えんどう豆"]}
   {:id :green-beans :ja "いんげん" :en "Green beans" :group :vegetables :patterns [#"いんげん|インゲン"]}
   {:id :cucumber :ja "きゅうり" :en "Cucumber" :group :vegetables :patterns [#"きゅうり|キュウリ"]}
   {:id :eggplant :ja "なす" :en "Eggplant" :group :vegetables :patterns [#"なす|ナス|茄子"]}
   {:id :zucchini :ja "ズッキーニ" :en "Zucchini" :group :vegetables :patterns [#"ズッキーニ"]}
   {:id :lotus-root :ja "れんこん" :en "Lotus root" :group :vegetables :patterns [#"れんこん|蓮根"]}
   {:id :burdock :ja "ごぼう" :en "Burdock" :group :vegetables :patterns [#"ごぼう|牛蒡"]}
   {:id :taro :ja "里いも" :en "Taro" :group :vegetables :patterns [#"里いも|里芋"]}

   {:id :apple :ja "りんご" :en "Apple" :group :fruit :patterns [#"りんご|リンゴ"]}
   {:id :banana :ja "バナナ" :en "Banana" :group :fruit :patterns [#"バナナ"]}
   {:id :pear :ja "なし" :en "Pear" :group :fruit :patterns [#"なし|梨"]}
   {:id :peach :ja "もも" :en "Peach" :group :fruit :patterns [#"もも|桃"]}
   {:id :strawberry :ja "いちご" :en "Strawberry" :group :fruit :patterns [#"いちご|苺"]}
   {:id :orange :ja "オレンジ" :en "Orange" :group :fruit :patterns [#"オレンジ|みかん"]}
   {:id :kiwi :ja "キウイ" :en "Kiwi" :group :fruit :patterns [#"キウイ"]}
   {:id :grape :ja "ぶどう" :en "Grape" :group :fruit :patterns [#"ぶどう|葡萄"]}
   {:id :watermelon :ja "すいか" :en "Watermelon" :group :fruit :patterns [#"すいか|スイカ|西瓜"]}
   {:id :melon :ja "メロン" :en "Melon" :group :fruit :patterns [#"メロン"]}
   {:id :persimmon :ja "かき" :en "Persimmon" :group :fruit :patterns [#"柿|かき"]}
   {:id :mango :ja "マンゴー" :en "Mango" :group :fruit :patterns [#"マンゴー"]}
   {:id :avocado :ja "アボカド" :en "Avocado" :group :fruit :patterns [#"アボカド"]}

   {:id :tofu :ja "豆腐" :en "Tofu" :group :protein :patterns [#"豆腐"]}
   {:id :kinako :ja "きなこ" :en "Kinako" :group :protein :patterns [#"きなこ|きな粉"]}
   {:id :white-fish :ja "白身魚・鯛" :en "White fish / sea bream" :group :protein
    :patterns [#"鯛|タイ|おさかな|魚|たい(?=と|\s|、|。|$)"]}
   {:id :shirasu :ja "しらす" :en "Whitebait" :group :protein :patterns [#"しらす"]}
   {:id :chicken :ja "鶏肉・ささみ" :en "Chicken" :group :protein :patterns [#"ささみ|鶏肉|チキン"]}
   {:id :yogurt :ja "ヨーグルト" :en "Yogurt" :group :protein :patterns [#"ヨーグルト"]}
   {:id :natto :ja "納豆" :en "Natto" :group :protein :patterns [#"納豆"]}
   {:id :soy-milk :ja "豆乳" :en "Soy milk" :group :protein :patterns [#"豆乳"]}
   {:id :cheese :ja "チーズ" :en "Cheese" :group :protein :patterns [#"チーズ"]}
   {:id :salmon :ja "さけ・鮭" :en "Salmon" :group :protein :patterns [#"さけ|鮭|サーモン"]}
   {:id :tuna :ja "まぐろ・ツナ" :en "Tuna" :group :protein :patterns [#"まぐろ|マグロ|鮪|ツナ"]}
   {:id :cod :ja "たら" :en "Cod" :group :protein :patterns [#"タラ|鱈"]}
   {:id :pork :ja "豚肉" :en "Pork" :group :protein :patterns [#"豚肉|ポーク"]}
   {:id :beef :ja "牛肉" :en "Beef" :group :protein :patterns [#"牛肉|ビーフ"]}
   {:id :liver :ja "レバー" :en "Liver" :group :protein :patterns [#"レバー"]}
   {:id :sesame :ja "ごま" :en "Sesame" :group :protein :patterns [#"ごま|ゴマ|胡麻"]}
   {:id :egg-yolk :ja "卵黄" :en "Egg yolk" :group :protein :patterns [#"卵黄|黄身"]}
   {:id :egg-white :ja "卵白・全卵" :en "Egg white / whole egg" :group :protein
    :patterns [#"卵白|全卵|たまご|玉子|卵"]}

   {:id :nori :ja "のり" :en "Nori" :group :other
    :patterns [#"海苔|(?:^|[\s、,。])のり(?=[\s、,。]|$)"]}
   {:id :wakame :ja "わかめ" :en "Wakame" :group :other :patterns [#"わかめ|ワカメ|若布"]}
   {:id :hijiki :ja "ひじき" :en "Hijiki" :group :other :patterns [#"ひじき|ヒジキ"]}
   {:id :mushroom :ja "きのこ" :en "Mushrooms" :group :other :patterns [#"きのこ|しいたけ|しめじ|えのき|舞茸"]}])

(def allergy-catalog
  [{:id :egg :ja "卵" :en "Egg" :required? true :patterns [#"卵黄|黄身|卵白|全卵|たまご|玉子|卵"]}
   {:id :milk :ja "乳" :en "Milk" :required? true :patterns [#"ヨーグルト|チーズ|牛乳|乳製品"]}
   {:id :wheat :ja "小麦" :en "Wheat" :required? true :patterns [#"パン|小麦|うどん|そうめん|パスタ"]}
   {:id :buckwheat :ja "そば" :en "Buckwheat" :required? true :patterns [#"そば|蕎麦"]}
   {:id :peanut :ja "落花生" :en "Peanut" :required? true :patterns [#"落花生|ピーナッツ"]}
   {:id :walnut :ja "くるみ" :en "Walnut" :required? true :patterns [#"くるみ|クルミ"]}
   {:id :cashew :ja "カシューナッツ" :en "Cashew" :required? true :patterns [#"カシューナッツ|カシュー"]}
   {:id :shrimp :ja "えび" :en "Shrimp" :required? true :patterns [#"えび|エビ|海老"]}
   {:id :crab :ja "かに" :en "Crab" :required? true :patterns [#"かに|カニ|蟹"]}
   {:id :soy :ja "大豆" :en "Soy" :patterns [#"豆腐|きなこ|きな粉|大豆|豆乳|納豆"]}
   {:id :sesame :ja "ごま" :en "Sesame" :patterns [#"ごま|ゴマ|胡麻"]}
   {:id :fish :ja "魚" :en "Fish" :patterns [#"鯛|タイ|おさかな|魚|しらす|さけ|鮭|サーモン|さば|鯖|たい(?=と|\s|、|。|$)"]}
   {:id :chicken :ja "鶏肉" :en "Chicken" :patterns [#"ささみ|鶏肉|チキン"]}
   {:id :banana :ja "バナナ" :en "Banana" :patterns [#"バナナ"]}
   {:id :apple :ja "りんご" :en "Apple" :patterns [#"りんご|リンゴ"]}
   {:id :peach :ja "もも" :en "Peach" :patterns [#"もも|桃"]}
   {:id :orange :ja "オレンジ" :en "Orange" :patterns [#"オレンジ|みかん"]}
   {:id :kiwi :ja "キウイ" :en "Kiwi" :patterns [#"キウイ"]}])

(defn- matches? [patterns memo]
  (boolean (some #(re-find % (or memo "")) patterns)))

(defn- annotate-item [events item]
  (let [matches (filter #(matches? (:patterns item) (:memo %)) events)]
    (assoc (dissoc item :patterns)
           :tried? (boolean (seq matches))
           :first-event (first matches)
           :last-event (last matches)
           :times (count matches))))

(defn analyze [events]
  (let [total-events (count events)
        events (->> events
                    (filter #(not (str/blank? (:memo %))))
                    (sort-by :event_time)
                    vec)
        ingredients (mapv #(annotate-item events %) ingredient-catalog)
        allergens (mapv #(annotate-item events %) allergy-catalog)]
    {:total-events total-events
     :meals events
     :ingredients ingredients
     :tried (filterv :tried? ingredients)
     :untried (filterv (complement :tried?) ingredients)
     :allergens allergens}))
