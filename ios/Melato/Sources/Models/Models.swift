import Foundation

struct Product: Identifiable, Hashable {
    let id: String
    let handle: String
    let title: String
    let price: Double
    let currency: String
    let description: String
    let imageUrl: String
    let category: String
    let sizes: [String]
    let colors: [String]
    let isFeatured: Bool
    let isNew: Bool
    let tags: [String]

    init(id: String, handle: String, title: String, price: Double,
         description: String, imageUrl: String, category: String,
         sizes: [String], colors: [String],
         isFeatured: Bool = false, isNew: Bool = false, tags: [String] = [],
         currency: String = "CAD") {
        self.id = id; self.handle = handle; self.title = title
        self.price = price; self.currency = currency
        self.description = description; self.imageUrl = imageUrl
        self.category = category; self.sizes = sizes; self.colors = colors
        self.isFeatured = isFeatured; self.isNew = isNew; self.tags = tags
    }

    var formattedPrice: String { String(format: "$%.2f CAD", price) }
    var productUrl: String { "https://melato.ca/products/\(handle)" }
}

struct CartItem: Identifiable {
    let id = UUID()
    let product: Product
    let size: String
    let color: String
    var quantity: Int

    var lineTotal: Double { product.price * Double(quantity) }
}

struct Category: Identifiable {
    let id: String
    let name: String

    static let all: [Category] = [
        Category(id: "all",            name: "All"),
        Category(id: "tracksuits",     name: "Tracksuits"),
        Category(id: "denim",          name: "Denim"),
        Category(id: "tops",           name: "Tops"),
        Category(id: "accessories",    name: "Accessories"),
        Category(id: "eves-wardrobe",  name: "Eve's Wardrobe"),
    ]
}

struct FaqItem: Identifiable {
    let id: String
    let question: String
    let answer: String
}

struct PopupEvent: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let imageName: String?
    let ctaLabel: String
}
