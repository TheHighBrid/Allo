package com.melato.shop.data

import com.melato.shop.data.model.Category
import com.melato.shop.data.model.FaqItem
import com.melato.shop.data.model.PopupEvent
import com.melato.shop.data.model.Product

object ShopifyRepo {

    val categories = listOf(
        Category("all", "All"),
        Category("tracksuits", "Tracksuits"),
        Category("denim", "Denim"),
        Category("tops", "Tops"),
        Category("accessories", "Accessories"),
        Category("eves-wardrobe", "Eve's Wardrobe")
    )

    val popupEvents = listOf(
        PopupEvent("June 21", "Location announced 48h prior"),
        PopupEvent("July 21", "Location announced 48h prior"),
        PopupEvent("August 20", "Location announced 48h prior")
    )

    val products = listOf(
        // ── TRACKSUITS ──────────────────────────────────────────────────────────
        Product(
            id = "ojos-jacket",
            handle = "ojos-velour-track-jacket",
            title = "OJOS Velour Track Jacket",
            price = 109.99,
            description = "A premium velour track jacket featuring a burgundy, black, red, and mauve colorblock design. The piece includes a back graphic reading \"The Eyes Chico They Never Lie\" and branded sleeve embroidery. Designed as part of a matching tracksuit set.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Ojos"),
            isFeatured = true,
            isNew = false,
            tags = listOf("Bestseller"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-ojos-velour-track-jacket.jpg",
            accent = 0xFFB44040
        ),
        Product(
            id = "ojos-pant",
            handle = "ojos-velour-track-pants",
            title = "OJOS Velour Track Pants",
            price = 69.99,
            description = "Matching track pant to the OJOS Velour Track Jacket. Colorblock velour with elastic waist, tapered leg, and branded details. Limited drop — full set available.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Ojos"),
            tags = listOf("Pair with jacket"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-ojos-velour-track-pants.jpg",
            accent = 0xFFB44040
        ),
        Product(
            id = "ovum-jacket",
            handle = "ovum-satin-track-jacket-pearl",
            title = "OVUM Satin Track Jacket — Pearl",
            price = 159.99,
            description = "A premium track jacket featuring clean retro-sport structure and controlled color blocking. Built as the upper half of the Melato uniform: structured enough to hold shape, relaxed enough to move through the day.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Pearl"),
            isFeatured = true,
            isNew = true,
            tags = listOf("New Drop"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-ovum-satin-track-jacket-pearl.jpg",
            accent = 0xFFD4C9A8
        ),
        Product(
            id = "ovum-pant",
            handle = "ovum-satin-track-pant-pearl",
            title = "OVUM Satin Track Pant — Pearl",
            price = 149.99,
            description = "The matching satin track pant to the OVUM Jacket. Pearl finish, elastic waistband with adjustable drawstring, tapered leg. Full set: $309.98 CAD.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Pearl"),
            isNew = true,
            tags = listOf("New Drop"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-ovum-satin-track-pant-pearl.jpg",
            accent = 0xFFD4C9A8
        ),
        Product(
            id = "divididos-jacket",
            handle = "divididos-velour-track-jacket",
            title = "Divididos Velour Track Jacket",
            price = 89.99,
            description = "Heritage-inspired velour construction with a distinctive divided colorblock layout. Full front zip, structured collar, branded embroidery. Pairs with the Divididos Track Pant for the full set.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Divididos"),
            isFeatured = true,
            tags = listOf("Core"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-divididos-velour-track-jacket.jpg",
            accent = 0xFF5E4B8B
        ),
        Product(
            id = "divididos-pant",
            handle = "divididos-velour-track-pant",
            title = "Divididos Velour Track Pant",
            price = 59.99,
            description = "Matching track pant to the Divididos Velour Track Jacket. Heritage colorblock design, elastic waist, side pockets.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Divididos"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-divididos-velour-track-pant.jpg",
            accent = 0xFF5E4B8B
        ),
        Product(
            id = "conquista-jacket",
            handle = "conquista-velour-track-jacket",
            title = "Conquista Velour Track Jacket",
            price = 89.99,
            description = "Mission-coded color-blocking with controlled presence. Teal, purple, lime, orange, burgundy, and black colorblock layout with graphic back detailing. Relaxed athletic fit — choose true size or size up.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Conquista"),
            isFeatured = true,
            tags = listOf("Bold"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-conquista-velour-track-jacket.jpg",
            accent = 0xFF2A9D8F
        ),
        Product(
            id = "conquista-pant",
            handle = "conquista-velour-track-pant",
            title = "Conquista Velour Track Pant",
            price = 59.99,
            description = "Matching pant to the Conquista Track Jacket. Bold colorblock construction, elastic waist with drawstring, tapered leg.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Conquista"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-conquista-velour-track-pant.jpg",
            accent = 0xFF2A9D8F
        ),
        Product(
            id = "chu-jacket",
            handle = "chu-velour-track-jacket",
            title = "Chū Velour Track Jacket",
            price = 89.99,
            description = "Grey blue geometric colorblock velour jacket with structured silhouette. A quieter expression of the Melato tracksuit identity — still intentional, still limited.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Chū"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-chu-velour-track-jacket.jpg",
            accent = 0xFF6B8CAE
        ),
        Product(
            id = "chu-pant",
            handle = "chu-velour-track-pant",
            title = "Chū Velour Track Pant",
            price = 59.99,
            description = "Matching track pant to the Chū Velour Jacket. Grey blue geometric colorblock, elastic waist, tapered cut.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Chū"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-chu-velour-track-pant.jpg",
            accent = 0xFF6B8CAE
        ),
        Product(
            id = "passion-jacket",
            handle = "passion-fruit-velour-track-jacket",
            title = "Passion Fruit Velour Track Jacket",
            price = 89.99,
            description = "Earth tone, warm colorblock velour jacket. One of the more expressive pieces in the tracksuit lineup — worn alone or as a full set.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Passion Fruit"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-passion-fruit-velour-track-jacket.jpg",
            accent = 0xFFE07B39
        ),
        Product(
            id = "passion-pant",
            handle = "passion-fruit-velour-track-pant",
            title = "Passion Fruit Velour Track Pant",
            price = 59.99,
            description = "Matching pant to the Passion Fruit Velour Track Jacket. Earth tone colorblock, relaxed athletic fit.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Passion Fruit"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-passion-fruit-velour-track-pant.jpg",
            accent = 0xFFE07B39
        ),
        Product(
            id = "midas-jacket",
            handle = "midas-runner-track-jacket",
            title = "Midas Runner Track Jacket",
            price = 149.99,
            description = "The runner silhouette — lighter construction, longer hang, athletic performance shape. The Midas jacket sits above the core velour line in feel and finish.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Midas"),
            isFeatured = true,
            tags = listOf("Premium"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-midas-runner-track-jacket.jpg",
            accent = 0xFFC9A84C
        ),
        Product(
            id = "midas-pant",
            handle = "midas-runner-track-pants",
            title = "Midas Runner Track Pants",
            price = 89.99,
            description = "Matching runner track pant to the Midas jacket. Lightweight athletic construction, tapered leg, elastic waist with cord.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Midas"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-midas-runner-track-pants.jpg",
            accent = 0xFFC9A84C
        ),
        Product(
            id = "reliance-jacket",
            handle = "reliance-bamboo-track-jacket",
            title = "Reliance Bamboo Track Jacket",
            price = 99.99,
            description = "Bamboo-blend track jacket in black salmon colorblock. Softer hand than velour — worn close or relaxed. The Reliance set sits between performance and leisure.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Black Salmon"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-reliance-bamboo-track-jacket.jpg",
            accent = 0xFFE8A598
        ),
        Product(
            id = "reliance-pant",
            handle = "reliance-bamboo-track-pant",
            title = "Reliance Bamboo Track Pant",
            price = 59.99,
            description = "Bamboo-blend matching track pant. Black salmon colorblock, elastic waist, tapered leg.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Black Salmon"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-reliance-bamboo-track-pant.jpg",
            accent = 0xFFE8A598
        ),

        // ── DENIM ───────────────────────────────────────────────────────────────
        Product(
            id = "9-to-noir",
            handle = "9-to-noir-tailored-flare-pant",
            title = "9 to Noir Tailored Flare Pant",
            price = 74.99,
            description = "A tailored flare silhouette in deep noir. Cut with precision — from the office to the after. One of the most versatile pieces in the collection.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Noir"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-9-to-noir-tailored-flare-pant.jpg",
            accent = 0xFF1A1A2E
        ),
        Product(
            id = "hendrix-jean",
            handle = "hendrix-distressed-flare-jean",
            title = "Hendrix Distressed Flare Jean",
            price = 79.99,
            description = "Distressed flare denim with a vintage athletic soul. The Hendrix sits at the intersection of seventies denim culture and modern street — worn with anything.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Hendrix"),
            isFeatured = true,
            imageUrl = "https://melato.ca/cdn/shop/files/melato-hendrix-distressed-flare-jean.jpg",
            accent = 0xFF3D5A80
        ),
        Product(
            id = "berry-jean",
            handle = "berry-distressed-slim-jean",
            title = "Berry Distressed Slim Jean",
            price = 139.99,
            description = "Slim cut distressed denim in a rich berry wash. A premium take on the classic slim — the distressing is intentional, not decorative.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Berry"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-berry-distressed-slim-jean.jpg",
            accent = 0xFF6B2D5E
        ),
        Product(
            id = "dirt-rich",
            handle = "dirt-rich-denim-flare-jean",
            title = "Dirt Rich Denim Flare Jean",
            price = 89.99,
            description = "Raw-edge flare denim with earth-toned washes and heavy character. The Dirt Rich is built for those who wear their clothes, not just own them.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Dirt Rich"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-dirt-rich-denim-flare-jean.jpg",
            accent = 0xFF7B5E3C
        ),
        Product(
            id = "nuit-blanche",
            handle = "nuit-blanche-zip-flare-jean",
            title = "Nuit Blanche Zip Flare Jean",
            price = 85.99,
            description = "Zip-detail flare denim for the late-night crowd. The Nuit Blanche is a statement piece — wear it when the night calls for something more.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Nuit Blanche"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-nuit-blanche-zip-flare-jean.jpg",
            accent = 0xFF2C3E50
        ),
        Product(
            id = "off-white-lie",
            handle = "off-white-lie-flare-jean",
            title = "Off-White Lie Flare Jean",
            price = 89.99,
            description = "A pale, off-white flare denim that reads clean and deliberate. The Off-White Lie is effortless dressed up or down.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Off-White"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-off-white-lie-flare-jean.jpg",
            accent = 0xFFE8E4D9
        ),
        Product(
            id = "rosetta-jean",
            handle = "rosetta-distressed-flare-jean",
            title = "Rosetta Distressed Flare Jean",
            price = 79.99,
            description = "A feminine-leaning distressed flare with warm undertones. The Rosetta pairs easily across the collection — a dependable piece in any rotation.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Rosetta"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-rosetta-distressed-flare-jean.jpg",
            accent = 0xFFB07560
        ),
        Product(
            id = "casanegra",
            handle = "casanegra-leather-flare-zip-pants",
            title = "CasaNegra Leather Flare Zip Pants",
            price = 159.99,
            description = "Premium leather-look flare zip pants in deep black. The CasaNegra is a full presence piece — structured silhouette, bold finish, built to own a room.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Nero"),
            isFeatured = true,
            tags = listOf("Premium"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-casanegra-leather-flare-zip-pants.jpg",
            accent = 0xFF1C1C1C
        ),
        Product(
            id = "kech",
            handle = "kech-leather-flare-zip-pants",
            title = "Kech Leather Flare Zip Pants",
            price = 159.99,
            description = "Leather-look flare zip pants in a warmer tonal palette. The Kech offers the same premium construction as the CasaNegra with a different visual language.",
            category = "denim",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Kech"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-kech-leather-flare-zip-pants.jpg",
            accent = 0xFF8B6914
        ),

        // ── TOPS ────────────────────────────────────────────────────────────────
        Product(
            id = "barolo",
            handle = "barolo-tweed-zip-overshirt",
            title = "Barolo Tweed Zip Overshirt",
            price = 149.99,
            description = "A rich tweed zip overshirt in deep burgundy tones — the Barolo is designed to layer, to lead, and to last. Textured construction with a clean front zip.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Barolo"),
            isFeatured = true,
            imageUrl = "https://melato.ca/cdn/shop/files/melato-barolo-tweed-zip-overshirt.jpg",
            accent = 0xFF722F37
        ),
        Product(
            id = "bluefin",
            handle = "the-bluefin-relaxed-shirt",
            title = "Bluefin Relaxed Shirt",
            price = 45.99,
            description = "A clean, relaxed button-up in a quiet palette. The Bluefin is the everyday reach — effortless and considered without demanding attention.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Bluefin"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-bluefin-relaxed-shirt.jpg",
            accent = 0xFF4A7FA5
        ),
        Product(
            id = "moula",
            handle = "moula-caramel-cable-knit-polo",
            title = "Moula Caramel Cable Knit Polo",
            price = 129.99,
            description = "A cable knit polo in warm caramel tones. The Moula is luxury dressed casually — premium tactile construction, refined silhouette.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Caramel"),
            isFeatured = true,
            isNew = true,
            tags = listOf("New"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-moula-caramel-cable-knit-polo.jpg",
            accent = 0xFFC8903A
        ),
        Product(
            id = "olive-affair",
            handle = "olive-affair-retro-panel-jacket",
            title = "Olive Affair Retro Panel Jacket",
            price = 84.99,
            description = "A retro-paneled jacket in olive tones with a structured athletic cut. The Olive Affair is a utility piece built for daily movement.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Olive"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-olive-affair-retro-panel-jacket.jpg",
            accent = 0xFF556B2F
        ),
        Product(
            id = "taupe-secret",
            handle = "taupe-secret-retro-panel-jacket",
            title = "Taupe Secret Retro Panel Jacket",
            price = 84.99,
            description = "Retro panel jacket in taupe — a quieter take on the same athletic silhouette as the Olive Affair. Pairs across the denim and tracksuit collections.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Taupe"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-taupe-secret-retro-panel-jacket.jpg",
            accent = 0xFFB5A899
        ),
        Product(
            id = "smoke-signal",
            handle = "smoke-signal-spray-tee",
            title = "Smoke Signal Spray Tee",
            price = 74.99,
            description = "A spray-effect graphic tee with a considered fade. The Smoke Signal is the foundational layer — wear it under a jacket, over a bodysuit, or on its own.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Smoke"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-smoke-signal-spray-tee.jpg",
            accent = 0xFF696969
        ),
        Product(
            id = "taqburni",
            handle = "taqburni-velour-polo-shirt",
            title = "Taqburni Velour Polo Shirt",
            price = 99.99,
            description = "A velour polo shirt that bridges the gap between the tracksuit line and tailored tops. The Taqburni is a Melato original — textured, intentional, limited.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Taqburni"),
            isNew = true,
            tags = listOf("New"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-taqburni-velour-polo-shirt.jpg",
            accent = 0xFF8B0000
        ),

        // ── ACCESSORIES ─────────────────────────────────────────────────────────
        Product(
            id = "atlas-dopp",
            handle = "atlas-dopp-kit",
            title = "Atlas Dopp Kit",
            price = 69.99,
            description = "A compact, structured travel kit in premium materials. The Atlas Dopp Kit is finished to the same standard as the apparel — carry it with intent.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Atlas"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-atlas-dopp-kit.jpg",
            accent = 0xFF5C4033
        ),
        Product(
            id = "atlas-duffle",
            handle = "atlas-woven-duffle",
            title = "Atlas Woven Duffle",
            price = 189.99,
            description = "A woven duffle bag with premium hardware and structured construction. Built for the traveler who doesn't compromise — generous capacity, deliberate design.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Atlas"),
            isFeatured = true,
            imageUrl = "https://melato.ca/cdn/shop/files/melato-atlas-woven-duffle.jpg",
            accent = 0xFF5C4033
        ),
        Product(
            id = "emerald-tie",
            handle = "emerald-solstice-tile-geometric-tie",
            title = "Emerald Solstice Tile Geometric Tie",
            price = 79.99,
            description = "A geometric tile pattern tie in emerald tones. The Emerald Solstice is part of the Melato accessories line — sharp, considered, limited.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Emerald"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-emerald-solstice-tile-geometric-tie.jpg",
            accent = 0xFF2D6A4F
        ),
        Product(
            id = "knotorious",
            handle = "knotorious-monogram-tie",
            title = "Knotorious Monogram Tie",
            price = 89.99,
            description = "A monogram tie with an all-over Melato pattern. The Knotorious is a statement piece — wearable brand identity at its most deliberate.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Knotorious"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-knotorious-monogram-tie.jpg",
            accent = 0xFF1B1B2F
        ),
        Product(
            id = "rosewood-tie",
            handle = "rosewood-checkerboard-tie",
            title = "Rosewood Checkerboard Tie",
            price = 79.99,
            description = "Checkerboard pattern tie in rosewood tones. Clean geometry, warm colour — a secondary statement that finishes any formal or semi-formal look.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Rosewood"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-rosewood-checkerboard-tie.jpg",
            accent = 0xFF7D3C3C
        ),
        Product(
            id = "ruby-tie",
            handle = "ruby-labyrinth-geometric-tie",
            title = "Ruby Labyrinth Geometric Tie",
            price = 79.99,
            description = "A deep ruby geometric labyrinth tie. Bold pattern, refined execution — the Ruby Labyrinth finishes a look or starts a conversation.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Ruby"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-ruby-labyrinth-geometric-tie.jpg",
            accent = 0xFF8B0000
        ),
        Product(
            id = "sunlit-tie",
            handle = "sunlit-atrium-geometric-tie",
            title = "Sunlit Atrium Geometric Tie",
            price = 79.99,
            description = "A warm, golden-hour geometric tie in sunlit tones. The Atrium is lighter in palette — a versatile accent that works across the collection.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Sunlit"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-sunlit-atrium-geometric-tie.jpg",
            accent = 0xFFC9A84C
        ),
        Product(
            id = "ivy-harness",
            handle = "ivy-trellis-harness-suspenders",
            title = "Ivy Trellis Harness Suspenders",
            price = 79.99,
            description = "Structured harness suspenders in an ivy trellis pattern. Wearable as a fashion accessory or functional piece — the Ivy Trellis is a collector's item.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Ivy"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-ivy-trellis-harness-suspenders.jpg",
            accent = 0xFF3A6351
        ),
        Product(
            id = "salopette",
            handle = "salopette-de-salo-harness-suspenders",
            title = "Salopette de Salo Harness Suspenders",
            price = 79.99,
            description = "A fashion-forward harness suspender set with architectural construction. The Salopette de Salo is a Melato original — worn as a centrepiece or layering accent.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Salo"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-salopette-de-salo-harness-suspenders.jpg",
            accent = 0xFF4A4A4A
        ),

        // ── EVE'S WARDROBE ───────────────────────────────────────────────────────
        Product(
            id = "blue-hour",
            handle = "blue-hour-alibi-mesh-bodysuit",
            title = "Blue Hour Alibi Mesh Bodysuit",
            price = 199.99,
            description = "A sheer mesh bodysuit designed for the blue hour — the quiet drama between day and night. Premium construction, intentional coverage, an unapologetic silhouette.",
            category = "eves-wardrobe",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Blue Hour"),
            isFeatured = true,
            isNew = true,
            tags = listOf("Eve's Wardrobe"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-blue-hour-alibi-mesh-bodysuit.jpg",
            accent = 0xFF1A3A5C
        ),
        Product(
            id = "body-language",
            handle = "body-language-mesh-bodysuit",
            title = "Body Language Mesh Bodysuit",
            price = 199.99,
            description = "Mesh bodysuit with deliberate cut and controlled exposure. The Body Language speaks for itself — wear it loud or quiet, but wear it on purpose.",
            category = "eves-wardrobe",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Body Language"),
            tags = listOf("Eve's Wardrobe"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-body-language-mesh-bodysuit.jpg",
            accent = 0xFF2D2D2D
        ),
        Product(
            id = "cognac-skirt",
            handle = "cognac-confession-leather-pencil-skirt",
            title = "Cognac Confession Leather Pencil Skirt",
            price = 130.00,
            description = "A leather pencil skirt in cognac — precise, bold, and entirely deliberate. Pairs with bodysuits, the Taqburni polo, or anything with presence.",
            category = "eves-wardrobe",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Cognac"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-cognac-confession-leather-pencil-skirt.jpg",
            accent = 0xFF9B5A2A
        ),
        Product(
            id = "noir-alibi",
            handle = "noir-alibi-lace-corset-dress",
            title = "Noir Alibi Lace Corset Dress",
            price = 319.99,
            description = "The statement piece of Eve's Wardrobe. A lace corset dress in noir with structured boning and deliberate silhouette. Limited production — not a restock piece.",
            category = "eves-wardrobe",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Noir"),
            isFeatured = true,
            isNew = true,
            tags = listOf("Eve's Wardrobe", "Statement"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-noir-alibi-lace-corset-dress.jpg",
            accent = 0xFF0D0D0D
        ),
        Product(
            id = "noir-memoir",
            handle = "noir-memoir-wrap-pencil-skirt",
            title = "Noir Memoir Wrap Pencil Skirt",
            price = 110.00,
            description = "A wrap pencil skirt in noir with a clean, tailored finish. The Noir Memoir is the understated option in Eve's Wardrobe — every bit as intentional.",
            category = "eves-wardrobe",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Noir"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-noir-memoir-wrap-pencil-skirt.jpg",
            accent = 0xFF1C1C1C
        ),
        Product(
            id = "dawn-to-dusk",
            handle = "dawn-to-dusk-three-piece-undergarment-set",
            title = "Dawn-to-Dusk Three Piece Undergarment Set",
            price = 119.99,
            description = "A three-piece undergarment set designed for all-day comfort and intentional styling. The Dawn-to-Dusk is a complete layering solution — morning through night.",
            category = "eves-wardrobe",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Ivory", "Noir"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-dawn-to-dusk-three-piece-undergarment-set.jpg",
            accent = 0xFFF5ECD7
        ),
        Product(
            id = "side-note",
            handle = "side-note-lace-rib-top",
            title = "Side Note Lace Rib Top",
            price = 59.99,
            description = "A lace rib top that layers under everything or stands alone. The Side Note is the entry point to Eve's Wardrobe — subtle, tactile, considered.",
            category = "eves-wardrobe",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Ivory", "Noir"),
            imageUrl = "https://melato.ca/cdn/shop/files/melato-side-note-lace-rib-top.jpg",
            accent = 0xFFD4C5B2
        )
    )

    val faq = listOf(
        FaqItem("How long does order processing take?",
            "Orders are usually prepared within 1 to 3 business days. During limited drops, holidays, or high-volume periods, processing may take slightly longer."),
        FaqItem("How long does shipping take?",
            "Delivery times depend on destination and selected shipping method. Final shipping options and estimated delivery windows appear at checkout."),
        FaqItem("Do you offer complimentary shipping?",
            "Yes — complimentary shipping on all orders, no minimum required."),
        FaqItem("Do you ship internationally?",
            "Yes — we ship worldwide with complimentary standard shipping. Any duties or import taxes, where applicable, are calculated at checkout or collected by the carrier."),
        FaqItem("Will I receive tracking?",
            "Yes. Once your order ships, tracking details are sent to the email or phone number used at checkout. Tracking may take up to 24 hours to update after the carrier receives the package."),
        FaqItem("Can I change or cancel my order?",
            "Contact support@melato.ca immediately. We'll do our best to help before fulfillment — once an order has been processed or shipped, changes may no longer be possible."),
        FaqItem("How does Melato fit?",
            "Melato pieces are designed with a relaxed athletic fit. Choose your usual size for a clean silhouette, or size up if you prefer a looser look."),
        FaqItem("What materials are used?",
            "Materials vary by product and collection. Each product page includes its own fabric composition and care notes where available."),
        FaqItem("How do I care for my Melato pieces?",
            "Wash cold on a gentle cycle, wash inside out, avoid bleach, and hang dry when possible. Do not iron directly over embroidery, prints, appliqués, or branded details."),
        FaqItem("Will sold-out colorways restock?",
            "Melato drops are limited. Some pieces may restock, but many colorways are released in small quantities and may not return once sold out."),
        FaqItem("What is your return window?",
            "Eligible items may be returned within 30 days of delivery. Items must be unworn, unused, unwashed, with original tags attached, and in original packaging."),
        FaqItem("How do I start a return?",
            "Email support@melato.ca with your order number, item details, and return reason. Our team will review and provide next steps."),
        FaqItem("Do you offer exchanges?",
            "Exchanges depend on availability. If the item you want is limited or close to selling out, the fastest option is usually to place a new order and return the original eligible item."),
        FaqItem("What if my item arrives damaged or incorrect?",
            "Contact support@melato.ca immediately with your order number and photos. We'll assess and resolve quickly.")
    )

    fun getProductById(id: String) = products.find { it.id == id }
    fun getByCategory(cat: String) = if (cat == "all") products else products.filter { it.category == cat }
    fun getFeatured() = products.filter { it.isFeatured }
    fun getNew() = products.filter { it.isNew }
    fun search(q: String): List<Product> {
        val query = q.lowercase()
        return products.filter {
            it.title.lowercase().contains(query) ||
            it.category.lowercase().contains(query) ||
            it.description.lowercase().contains(query)
        }
    }
}
