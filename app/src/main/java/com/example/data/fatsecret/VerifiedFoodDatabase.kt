package com.example.data.fatsecret

import com.example.data.entity.FoodItem

object VerifiedFoodDatabase {

    val items: List<FoodItem> = listOf(
        // CARNI & POLLAME
        FoodItem("v_pollo", "Petto di pollo", "Fresco", "100g", 100.0, "g", 110.0, 23.3, 0.0, 1.2, "verified_db"),
        FoodItem("v_tacchino", "Fesa di tacchino", "Fresco", "100g", 100.0, "g", 107.0, 24.0, 0.0, 1.2, "verified_db"),
        FoodItem("v_manzo_magro", "Manzo magro (Bistecca)", "Fresco", "100g", 100.0, "g", 127.0, 21.5, 0.0, 4.5, "verified_db"),
        FoodItem("v_macinato_manzo", "Macinato di manzo scelto", "Fresco", "100g", 100.0, "g", 176.0, 20.0, 0.0, 10.5, "verified_db"),
        FoodItem("v_bresaola", "Bresaola della Valtellina IGP", "Salumi", "100g", 100.0, "g", 151.0, 32.0, 0.1, 2.6, "verified_db"),
        FoodItem("v_crudo_sgrassato", "Prosciutto crudo sgrassato", "Salumi", "100g", 100.0, "g", 145.0, 28.0, 0.2, 3.5, "verified_db"),
        FoodItem("v_cotto_scelto", "Prosciutto cotto alta qualità", "Salumi", "100g", 100.0, "g", 132.0, 19.8, 0.8, 5.5, "verified_db"),
        FoodItem("v_arista", "Arista di maiale", "Fresco", "100g", 100.0, "g", 143.0, 21.0, 0.0, 6.5, "verified_db"),
        FoodItem("v_coniglio", "Carne di coniglio", "Fresco", "100g", 100.0, "g", 118.0, 20.0, 0.0, 4.3, "verified_db"),

        // PESCE & FRUTTI DI MARE
        FoodItem("v_salmone", "Salmone fresco", "Pesce", "100g", 100.0, "g", 185.0, 20.0, 0.0, 12.0, "verified_db"),
        FoodItem("v_tonno_nat", "Tonno al naturale", "Conserve", "100g", 100.0, "g", 103.0, 24.0, 0.0, 0.8, "verified_db"),
        FoodItem("v_tonno_olio", "Tonno all'olio sgocciolato", "Conserve", "100g", 100.0, "g", 192.0, 25.5, 0.0, 10.0, "verified_db"),
        FoodItem("v_merluzzo", "Merluzzo (Nasello)", "Pesce", "100g", 100.0, "g", 71.0, 17.0, 0.0, 0.3, "verified_db"),
        FoodItem("v_orata", "Orata fresca", "Pesce", "100g", 100.0, "g", 121.0, 20.7, 0.0, 3.8, "verified_db"),
        FoodItem("v_spigola", "Spigola (Branzino)", "Pesce", "100g", 100.0, "g", 82.0, 16.5, 0.6, 1.5, "verified_db"),
        FoodItem("v_pesce_spada", "Pesce spada", "Pesce", "100g", 100.0, "g", 109.0, 19.8, 0.0, 3.5, "verified_db"),
        FoodItem("v_gamberi", "Gamberi / Mazzancolle", "Crostacei", "100g", 100.0, "g", 85.0, 17.6, 0.5, 1.1, "verified_db"),
        FoodItem("v_polpo", "Polpo lessato", "Molluschi", "100g", 100.0, "g", 70.0, 15.0, 1.0, 0.7, "verified_db"),
        FoodItem("v_sgombro", "Sgombro fresco", "Pesce", "100g", 100.0, "g", 170.0, 17.0, 0.0, 11.0, "verified_db"),

        // UOVA & LATTICINI
        FoodItem("v_uovo_intero", "Uovo intero", "Fresco", "100g", 100.0, "g", 128.0, 12.4, 0.5, 8.7, "verified_db"),
        FoodItem("v_albume", "Albume d'uovo", "Fresco", "100g", 100.0, "g", 43.0, 10.9, 0.7, 0.1, "verified_db"),
        FoodItem("v_yogurt_greco_0", "Yogurt greco 0% grassi", "Fage / Total", "100g", 100.0, "g", 54.0, 10.3, 3.0, 0.0, "verified_db"),
        FoodItem("v_yogurt_greco_2", "Yogurt greco 2% grassi", "Fage", "100g", 100.0, "g", 73.0, 9.9, 3.8, 2.0, "verified_db"),
        FoodItem("v_yogurt_bianco", "Yogurt bianco intero", "Latticini", "100g", 100.0, "g", 66.0, 3.8, 4.3, 3.9, "verified_db"),
        FoodItem("v_fiocchi_latte", "Fiocchi di latte (Cottage)", "Latticini", "100g", 100.0, "g", 85.0, 12.5, 2.8, 2.5, "verified_db"),
        FoodItem("v_ricotta_vaccina", "Ricotta vaccina", "Latticini", "100g", 100.0, "g", 146.0, 8.8, 3.5, 10.9, "verified_db"),
        FoodItem("v_parmigiano", "Parmigiano Reggiano DOP", "Formaggi", "100g", 100.0, "g", 392.0, 33.0, 0.0, 28.4, "verified_db"),
        FoodItem("v_grana", "Grana Padano DOP", "Formaggi", "100g", 100.0, "g", 384.0, 33.0, 0.0, 28.0, "verified_db"),
        FoodItem("v_mozzarella", "Mozzarella vaccina", "Formaggi", "100g", 100.0, "g", 253.0, 18.7, 0.7, 19.5, "verified_db"),
        FoodItem("v_mozzarella_light", "Mozzarella light", "Formaggi", "100g", 100.0, "g", 165.0, 20.5, 1.0, 8.5, "verified_db"),
        FoodItem("v_latte_ps", "Latte parzialmente scremato", "Bevande", "100ml", 100.0, "ml", 46.0, 3.3, 4.8, 1.6, "verified_db"),
        FoodItem("v_latte_intero", "Latte intero", "Bevande", "100ml", 100.0, "ml", 64.0, 3.3, 4.7, 3.6, "verified_db"),
        FoodItem("v_latte_soia", "Latte di soia (senza zuccheri)", "Bevande veg", "100ml", 100.0, "ml", 33.0, 3.3, 0.2, 1.8, "verified_db"),
        FoodItem("v_latte_mandorla", "Latte di mandorla senza zuccheri", "Bevande veg", "100ml", 100.0, "ml", 13.0, 0.5, 0.2, 1.1, "verified_db"),

        // CEREALI, PASTA & PANE
        FoodItem("v_pasta_semola", "Pasta di semola di grano duro", "Barilla / De Cecco", "100g", 100.0, "g", 356.0, 12.5, 71.7, 1.5, "verified_db"),
        FoodItem("v_pasta_integrale", "Pasta integrale", "Barilla", "100g", 100.0, "g", 348.0, 13.0, 65.0, 2.5, "verified_db"),
        FoodItem("v_riso_basmati", "Riso Basmati", "Cereali", "100g", 100.0, "g", 352.0, 8.5, 77.0, 0.9, "verified_db"),
        FoodItem("v_riso_bianco", "Riso bianco Carnaroli", "Cereali", "100g", 100.0, "g", 358.0, 6.7, 80.4, 0.6, "verified_db"),
        FoodItem("v_riso_integrale", "Riso integrale", "Cereali", "100g", 100.0, "g", 340.0, 7.5, 72.0, 2.2, "verified_db"),
        FoodItem("v_avena", "Fiocchi d'avena (Porridge)", "Quaker / Generico", "100g", 100.0, "g", 372.0, 13.5, 60.0, 7.0, "verified_db"),
        FoodItem("v_pane_comune", "Pane comune tipo 0", "Panetteria", "100g", 100.0, "g", 270.0, 8.6, 54.0, 1.2, "verified_db"),
        FoodItem("v_pane_integrale", "Pane integrale", "Panetteria", "100g", 100.0, "g", 243.0, 9.0, 48.5, 1.5, "verified_db"),
        FoodItem("v_pane_segale", "Pane di segale", "Panetteria", "100g", 100.0, "g", 215.0, 6.5, 45.0, 1.0, "verified_db"),
        FoodItem("v_fette_biscottate", "Fette biscottate classiche", "Mulino Bianco", "100g", 100.0, "g", 396.0, 11.3, 72.0, 6.0, "verified_db"),
        FoodItem("v_gallette_riso", "Gallette di riso", "Snack", "100g", 100.0, "g", 380.0, 8.0, 81.0, 1.5, "verified_db"),
        FoodItem("v_gallette_mais", "Gallette di mais", "Snack", "100g", 100.0, "g", 375.0, 8.2, 79.0, 1.8, "verified_db"),
        FoodItem("v_couscous", "Couscous", "Cereali", "100g", 100.0, "g", 356.0, 12.0, 72.5, 1.5, "verified_db"),
        FoodItem("v_farro", "Farro perlato", "Cereali", "100g", 100.0, "g", 335.0, 14.5, 67.0, 2.5, "verified_db"),
        FoodItem("v_quinoa", "Quinoa", "Cereali", "100g", 100.0, "g", 368.0, 14.1, 64.2, 6.1, "verified_db"),
        FoodItem("v_gnocchi", "Gnocchi di patate freschi", "Pasta fresca", "100g", 100.0, "g", 160.0, 4.0, 34.0, 0.8, "verified_db"),
        FoodItem("v_patate", "Patate lesse / al vapore", "Tuberi", "100g", 100.0, "g", 83.0, 2.1, 18.0, 0.1, "verified_db"),
        FoodItem("v_patate_dolci", "Patate dolci (Americane)", "Tuberi", "100g", 100.0, "g", 90.0, 1.6, 21.0, 0.2, "verified_db"),

        // GRASSI, OLIO & FRUTTA SECCA
        FoodItem("v_olio_evo", "Olio Extravergine d'Oliva (EVO)", "Condimento", "10g", 10.0, "g", 884.0, 0.0, 0.0, 100.0, "verified_db"),
        FoodItem("v_mandorle", "Mandorle sgusciate", "Frutta secca", "100g", 100.0, "g", 595.0, 21.1, 21.6, 50.0, "verified_db"),
        FoodItem("v_noci", "Noci sgusciate", "Frutta secca", "100g", 100.0, "g", 654.0, 15.2, 13.7, 65.2, "verified_db"),
        FoodItem("v_burro_arachidi", "Burro d'arachidi 100%", "Frutta secca", "100g", 100.0, "g", 588.0, 25.0, 20.0, 50.0, "verified_db"),
        FoodItem("v_avocado", "Avocado fresco", "Frutta", "100g", 100.0, "g", 160.0, 2.0, 9.0, 15.0, "verified_db"),
        FoodItem("v_cioccolato_85", "Cioccolato fondente 85%", "Dolci", "100g", 100.0, "g", 580.0, 8.5, 19.0, 51.0, "verified_db"),
        FoodItem("v_semi_chia", "Semi di chia", "Semi", "100g", 100.0, "g", 486.0, 16.5, 42.1, 30.7, "verified_db"),
        FoodItem("v_semi_lino", "Semi di lino", "Semi", "100g", 100.0, "g", 534.0, 18.3, 28.9, 42.2, "verified_db"),

        // LEGUMI
        FoodItem("v_lenticchie_secche", "Lenticchie secche", "Legumi", "100g", 100.0, "g", 319.0, 24.7, 49.9, 1.0, "verified_db"),
        FoodItem("v_lenticchie_scatola", "Lenticchie lessate in scatola", "Legumi", "100g", 100.0, "g", 82.0, 6.7, 12.0, 0.5, "verified_db"),
        FoodItem("v_ceci_lessati", "Ceci lessati in scatola", "Legumi", "100g", 100.0, "g", 115.0, 7.0, 17.5, 2.0, "verified_db"),
        FoodItem("v_fagioli_borlotti", "Fagioli borlotti lessati", "Legumi", "100g", 100.0, "g", 91.0, 6.5, 14.5, 0.6, "verified_db"),
        FoodItem("v_fagioli_cannellini", "Fagioli cannellini lessati", "Legumi", "100g", 100.0, "g", 88.0, 6.8, 14.0, 0.5, "verified_db"),
        FoodItem("v_piselli", "Piselli fini (cotti)", "Legumi", "100g", 100.0, "g", 75.0, 5.5, 11.5, 0.4, "verified_db"),
        FoodItem("v_edamame", "Edamame (Soia verde)", "Legumi", "100g", 100.0, "g", 122.0, 11.0, 10.0, 5.0, "verified_db"),

        // FRUTTA
        FoodItem("v_mela", "Mela con buccia", "Frutta", "100g", 100.0, "g", 52.0, 0.3, 13.8, 0.2, "verified_db"),
        FoodItem("v_banana", "Banana fresca", "Frutta", "100g", 100.0, "g", 89.0, 1.1, 22.8, 0.3, "verified_db"),
        FoodItem("v_arancia", "Arancia fresca", "Frutta", "100g", 100.0, "g", 47.0, 0.9, 11.8, 0.1, "verified_db"),
        FoodItem("v_fragole", "Fragole fresche", "Frutta", "100g", 100.0, "g", 32.0, 0.7, 7.7, 0.3, "verified_db"),
        FoodItem("v_mirtilli", "Mirtilli freschi", "Frutta", "100g", 100.0, "g", 57.0, 0.7, 14.5, 0.3, "verified_db"),
        FoodItem("v_kiwi", "Kiwi fresco", "Frutta", "100g", 100.0, "g", 61.0, 1.1, 14.7, 0.5, "verified_db"),
        FoodItem("v_pera", "Pera fresca", "Frutta", "100g", 100.0, "g", 57.0, 0.4, 15.2, 0.1, "verified_db"),
        FoodItem("v_pesca", "Pesca fresca", "Frutta", "100g", 100.0, "g", 39.0, 0.9, 9.5, 0.2, "verified_db"),
        FoodItem("v_ananas", "Ananas fresco", "Frutta", "100g", 100.0, "g", 50.0, 0.5, 13.1, 0.1, "verified_db"),
        FoodItem("v_anguria", "Anguria fresca", "Frutta", "100g", 100.0, "g", 30.0, 0.6, 7.6, 0.2, "verified_db"),

        // VERDURA
        FoodItem("v_spinaci", "Spinaci freschi", "Verdura", "100g", 100.0, "g", 23.0, 2.9, 3.6, 0.4, "verified_db"),
        FoodItem("v_broccoli", "Broccoli cotti al vapore", "Verdura", "100g", 100.0, "g", 35.0, 2.4, 7.2, 0.4, "verified_db"),
        FoodItem("v_zucchine", "Zucchine fresche", "Verdura", "100g", 100.0, "g", 17.0, 1.2, 3.1, 0.3, "verified_db"),
        FoodItem("v_finocchi", "Finocchi freschi", "Verdura", "100g", 100.0, "g", 15.0, 1.2, 1.0, 0.2, "verified_db"),
        FoodItem("v_carote", "Carote fresche", "Verdura", "100g", 100.0, "g", 41.0, 0.9, 9.6, 0.2, "verified_db"),
        FoodItem("v_pomodori", "Pomodori da insalata", "Verdura", "100g", 100.0, "g", 18.0, 0.9, 3.9, 0.2, "verified_db"),
        FoodItem("v_peperoni", "Peperoni rossi e gialli", "Verdura", "100g", 100.0, "g", 26.0, 1.0, 6.0, 0.3, "verified_db"),
        FoodItem("v_melanzane", "Melanzane", "Verdura", "100g", 100.0, "g", 25.0, 1.0, 5.9, 0.2, "verified_db"),
        FoodItem("v_insalata", "Insalata mista / Lattuga", "Verdura", "100g", 100.0, "g", 15.0, 1.4, 2.2, 0.2, "verified_db"),
        FoodItem("v_funghi", "Funghi champignon", "Verdura", "100g", 100.0, "g", 22.0, 3.1, 3.3, 0.3, "verified_db"),

        // INTEGRATORI & PROTEINE
        FoodItem("v_whey", "Proteine Whey (Isolate/Concentrate)", "Integratore", "30g", 30.0, "g", 380.0, 80.0, 5.0, 3.5, "verified_db"),
        FoodItem("v_caseina", "Proteine Caseine Micellari", "Integratore", "30g", 30.0, "g", 365.0, 78.0, 4.5, 2.0, "verified_db"),
        FoodItem("v_miele", "Miele millefiori", "Dolcificante", "100g", 100.0, "g", 304.0, 0.3, 82.4, 0.0, "verified_db"),
        FoodItem("v_marmellata", "Marmellata 100% frutta", "Confettura", "100g", 100.0, "g", 145.0, 0.5, 35.0, 0.1, "verified_db")
    )

    fun search(query: String): List<FoodItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return items.take(15)
        return items.filter {
            it.name.lowercase().contains(q) || (it.brand?.lowercase()?.contains(q) == true)
        }.sortedBy {
            val idx = it.name.lowercase().indexOf(q)
            if (idx == 0) 0 else if (idx > 0) 1 else 2
        }
    }

    /**
     * Finds the best certified FoodItem match for an ingredient name,
     * resolving synonyms, singular/plural, and typical Italian fitness food terms.
     */
    fun findBestMatch(query: String): FoodItem? {
        val raw = query.trim().lowercase()
        if (raw.isBlank()) return null

        // Direct search match first
        val directMatches = search(raw)
        if (directMatches.isNotEmpty() && (directMatches.first().name.lowercase() == raw || raw.contains(directMatches.first().name.lowercase()))) {
            return directMatches.first()
        }

        // Semantic synonym and keyword resolution
        return when {
            raw.contains("albume") || raw.contains("albumi") || raw.contains("bianco d'uovo") ->
                items.find { it.id == "v_albume" }
            raw.contains("uovo") || raw.contains("uova") ->
                items.find { it.id == "v_uovo_intero" }
            raw.contains("fage") || (raw.contains("yogurt") && raw.contains("greco") && (raw.contains("0") || raw.contains("zero") || raw.contains("magro"))) ->
                items.find { it.id == "v_yogurt_greco_0" }
            raw.contains("yogurt") && raw.contains("greco") ->
                items.find { it.id == "v_yogurt_greco_2" }
            raw.contains("yogurt") ->
                items.find { it.id == "v_yogurt_bianco" }
            (raw.contains("petto") && raw.contains("pollo")) || raw.contains("pollo") ->
                items.find { it.id == "v_pollo" }
            raw.contains("tacchino") || raw.contains("fesa") ->
                items.find { it.id == "v_tacchino" }
            raw.contains("manzo") || raw.contains("bistecca") || raw.contains("scottona") ->
                items.find { it.id == "v_manzo_magro" }
            raw.contains("macinato") ->
                items.find { it.id == "v_macinato_manzo" }
            raw.contains("salmone") ->
                items.find { it.id == "v_salmone" }
            (raw.contains("tonno") && (raw.contains("nat") || raw.contains("sgocc") || raw.contains("scatola") || !raw.contains("olio"))) ->
                items.find { it.id == "v_tonno_nat" }
            raw.contains("tonno") ->
                items.find { it.id == "v_tonno_olio" }
            raw.contains("merluzzo") || raw.contains("nasello") ->
                items.find { it.id == "v_merluzzo" }
            raw.contains("orata") ->
                items.find { it.id == "v_orata" }
            raw.contains("spigola") || raw.contains("branzino") ->
                items.find { it.id == "v_spigola" }
            raw.contains("gamber") || raw.contains("mazzancoll") ->
                items.find { it.id == "v_gamberi" }
            raw.contains("bresaola") ->
                items.find { it.id == "v_bresaola" }
            raw.contains("prosciutto crudo") || raw.contains("crudo") ->
                items.find { it.id == "v_crudo_sgrassato" }
            raw.contains("prosciutto cotto") || raw.contains("cotto") ->
                items.find { it.id == "v_cotto_scelto" }
            raw.contains("olio") || raw.contains("evo") ->
                items.find { it.id == "v_olio_evo" }
            raw.contains("avena") || raw.contains("oatmeal") || raw.contains("porridge") ->
                items.find { it.id == "v_avena" }
            raw.contains("basmati") ->
                items.find { it.id == "v_riso_basmati" }
            raw.contains("riso") && (raw.contains("integrale") || raw.contains("nero") || raw.contains("venere")) ->
                items.find { it.id == "v_riso_integrale" }
            raw.contains("riso") ->
                items.find { it.id == "v_riso_basmati" }
            raw.contains("pasta") && raw.contains("integrale") ->
                items.find { it.id == "v_pasta_integrale" }
            raw.contains("pasta") || raw.contains("spaghetti") || raw.contains("penne") || raw.contains("fusilli") || raw.contains("rigatoni") ->
                items.find { it.id == "v_pasta_semola" }
            raw.contains("pane") && raw.contains("integrale") ->
                items.find { it.id == "v_pane_integrale" }
            raw.contains("pane") && (raw.contains("segale") || raw.contains("pema")) ->
                items.find { it.id == "v_pane_segale" }
            raw.contains("pane") || raw.contains("rosetta") || raw.contains("ciabatta") || raw.contains("baguette") ->
                items.find { it.id == "v_pane_comune" }
            raw.contains("fetta biscottata") || raw.contains("fette biscottate") ->
                items.find { it.id == "v_fette_biscottate" }
            raw.contains("gallett") && (raw.contains("mais") || raw.contains("legumi")) ->
                items.find { it.id == "v_gallette_mais" }
            raw.contains("gallett") ->
                items.find { it.id == "v_gallette_riso" }
            raw.contains("parmigiano") || raw.contains("grana") || raw.contains("parmigiana") ->
                items.find { it.id == "v_parmigiano" }
            raw.contains("mozzarella") && (raw.contains("light") || raw.contains("leggera")) ->
                items.find { it.id == "v_mozzarella_light" }
            raw.contains("mozzarella") ->
                items.find { it.id == "v_mozzarella" }
            raw.contains("ricotta") ->
                items.find { it.id == "v_ricotta_vaccina" }
            raw.contains("fiocchi di latte") || raw.contains("cottage") || raw.contains("jocca") ->
                items.find { it.id == "v_fiocchi_latte" }
            raw.contains("mandorl") ->
                items.find { it.id == "v_mandorle" }
            raw.contains("noci") || raw.contains("noce") ->
                items.find { it.id == "v_noci" }
            raw.contains("burro d'arachidi") || raw.contains("burro di arachidi") || raw.contains("peanut butter") ->
                items.find { it.id == "v_burro_arachidi" }
            raw.contains("avocado") ->
                items.find { it.id == "v_avocado" }
            raw.contains("mela") || raw.contains("mele") ->
                items.find { it.id == "v_mela" }
            raw.contains("banana") || raw.contains("banane") ->
                items.find { it.id == "v_banana" }
            raw.contains("arancia") || raw.contains("arance") ->
                items.find { it.id == "v_arancia" }
            raw.contains("fragol") ->
                items.find { it.id == "v_fragole" }
            raw.contains("mirtill") ->
                items.find { it.id == "v_mirtilli" }
            raw.contains("kiwi") ->
                items.find { it.id == "v_kiwi" }
            raw.contains("pera") || raw.contains("pere") ->
                items.find { it.id == "v_pera" }
            raw.contains("pesca") || raw.contains("pesche") ->
                items.find { it.id == "v_pesca" }
            raw.contains("ananas") ->
                items.find { it.id == "v_ananas" }
            raw.contains("patat") && (raw.contains("dolc") || raw.contains("american")) ->
                items.find { it.id == "v_patate_dolci" }
            raw.contains("patat") ->
                items.find { it.id == "v_patate" }
            raw.contains("gnocchi") ->
                items.find { it.id == "v_gnocchi" }
            raw.contains("spinac") ->
                items.find { it.id == "v_spinaci" }
            raw.contains("broccol") ->
                items.find { it.id == "v_broccoli" }
            raw.contains("zucchin") ->
                items.find { it.id == "v_zucchine" }
            raw.contains("pomodor") ->
                items.find { it.id == "v_pomodori" }
            raw.contains("carot") ->
                items.find { it.id == "v_carote" }
            raw.contains("insalat") || raw.contains("lattuga") ->
                items.find { it.id == "v_insalata" }
            raw.contains("lenticchi") ->
                items.find { it.id == "v_lenticchie_scatola" }
            raw.contains("ceci") ->
                items.find { it.id == "v_ceci_lessati" }
            raw.contains("fagiol") ->
                items.find { it.id == "v_fagioli_cannellini" }
            raw.contains("pisell") ->
                items.find { it.id == "v_piselli" }
            raw.contains("whey") || raw.contains("proteine in polvere") || (raw.contains("proteine") && raw.contains("isolate")) ->
                items.find { it.id == "v_whey" }
            raw.contains("miele") ->
                items.find { it.id == "v_miele" }
            raw.contains("marmellata") || raw.contains("confettura") ->
                items.find { it.id == "v_marmellata" }
            raw.contains("cioccolato") ->
                items.find { it.id == "v_cioccolato_85" }
            raw.contains("latte") && raw.contains("soia") ->
                items.find { it.id == "v_latte_soia" }
            raw.contains("latte") && raw.contains("mandorla") ->
                items.find { it.id == "v_latte_mandorla" }
            raw.contains("latte") ->
                items.find { it.id == "v_latte_ps" }
            else -> directMatches.firstOrNull()
        }
    }
}
