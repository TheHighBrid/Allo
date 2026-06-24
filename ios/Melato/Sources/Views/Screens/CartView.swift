import SwiftUI

struct CartView: View {
    @ObservedObject var cart: CartViewModel

    var body: some View {
        Group {
            if cart.items.isEmpty {
                emptyCart
            } else {
                filledCart
            }
        }
        .background(Color.nearBlack)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("YOUR BAG")
                    .font(.system(size: 14, weight: .bold))
                    .tracking(6)
                    .foregroundColor(.white)
            }
        }
    }

    private var emptyCart: some View {
        VStack(spacing: 16) {
            Image(systemName: "bag")
                .font(.system(size: 50))
                .foregroundColor(Color.gold.opacity(0.4))
            Text("Your bag is empty")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.textSecondary)
            Text("Add something worth wearing.")
                .font(.system(size: 13))
                .foregroundColor(.textMuted)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var filledCart: some View {
        VStack(spacing: 0) {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 1) {
                    ForEach(cart.items) { item in
                        CartItemRow(item: item, cart: cart)
                    }
                }
            }
            checkoutPanel
        }
    }

    private var checkoutPanel: some View {
        VStack(spacing: 0) {
            Divider().background(Color.surface3)
            VStack(spacing: 14) {
                HStack {
                    Text("TOTAL")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(3)
                        .foregroundColor(.textMuted)
                    Spacer()
                    Text(cart.formattedTotal)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                }
                Link(destination: URL(string: "https://melato.ca/cart")!) {
                    Text("CHECKOUT ON MELATO.CA")
                        .font(.system(size: 13, weight: .bold))
                        .tracking(3)
                        .foregroundColor(.nearBlack)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color.gold)
                        .clipShape(RoundedRectangle(cornerRadius: 4))
                }
            }
            .padding(20)
            .background(Color.surface1)
        }
    }
}

struct CartItemRow: View {
    let item: CartItem
    let cart: CartViewModel

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: URL(string: item.product.imageUrl)) { phase in
                switch phase {
                case .success(let img):
                    img.resizable().aspectRatio(contentMode: .fill)
                default:
                    Rectangle().fill(Color.surface3)
                        .overlay(Text("M").font(.system(size: 16, weight: .black)).foregroundColor(.gold))
                }
            }
            .frame(width: 80, height: 80)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 6))

            VStack(alignment: .leading, spacing: 4) {
                Text(item.product.title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white)
                    .lineLimit(2)
                HStack(spacing: 6) {
                    Text(item.size)
                        .font(.system(size: 11))
                        .foregroundColor(.textMuted)
                    Text("·")
                        .foregroundColor(.textMuted)
                    Text(item.color)
                        .font(.system(size: 11))
                        .foregroundColor(.textMuted)
                }
                Text(String(format: "$%.2f CAD", item.lineTotal))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.gold)
            }
            Spacer()
            VStack(spacing: 8) {
                Button {
                    cart.updateQuantity(item, delta: 1)
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 28, height: 28)
                        .background(Color.surface3)
                        .clipShape(Circle())
                }
                Text("\(item.quantity)")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white)
                Button {
                    cart.updateQuantity(item, delta: -1)
                } label: {
                    Image(systemName: "minus")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 28, height: 28)
                        .background(Color.surface3)
                        .clipShape(Circle())
                }
            }
        }
        .padding(14)
        .background(Color.surface1)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                withAnimation { cart.remove(item) }
            } label: {
                Label("Remove", systemImage: "trash")
            }
        }
    }
}
