import Foundation
import Combine

final class CartViewModel: ObservableObject {
    @Published private(set) var items: [CartItem] = []

    var count: Int { items.reduce(0) { $0 + $1.quantity } }
    var total: Double { items.reduce(0) { $0 + $1.lineTotal } }
    var formattedTotal: String { String(format: "$%.2f CAD", total) }

    func add(_ product: Product, size: String, color: String) {
        if let idx = items.firstIndex(where: { $0.product.id == product.id && $0.size == size && $0.color == color }) {
            items[idx].quantity += 1
        } else {
            items.append(CartItem(product: product, size: size, color: color, quantity: 1))
        }
    }

    func remove(_ item: CartItem) {
        items.removeAll { $0.id == item.id }
    }

    func updateQuantity(_ item: CartItem, delta: Int) {
        guard let idx = items.firstIndex(where: { $0.id == item.id }) else { return }
        let newQty = items[idx].quantity + delta
        if newQty <= 0 { items.remove(at: idx) } else { items[idx].quantity = newQty }
    }
}
