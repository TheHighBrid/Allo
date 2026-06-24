import SwiftUI

struct ProductDetailView: View {
    let product: Product
    let cart: CartViewModel

    @State private var selectedSize = ""
    @State private var selectedColor = ""
    @State private var addedToCart = false
    @Environment(\.dismiss) private var dismiss

    private let sizes = ["XS", "S", "M", "L", "XL", "XXL"]
    private let colors = ["Black", "White", "Gold", "Navy", "Olive"]

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                productImage
                VStack(alignment: .leading, spacing: 16) {
                    productHeader
                    Divider().background(Color.surface3)
                    if !product.sizes.isEmpty {
                        sizeSelector
                    }
                    if !product.colors.isEmpty {
                        colorSelector
                    }
                    addToCartButton
                    viewOnWebButton
                    productDetails
                }
                .padding(20)
            }
        }
        .background(Color.nearBlack)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(product.title.uppercased())
                    .font(.system(size: 11, weight: .bold))
                    .tracking(3)
                    .foregroundColor(.white)
                    .lineLimit(1)
            }
        }
        .onAppear {
            selectedSize = product.sizes.first ?? ""
            selectedColor = product.colors.first ?? ""
        }
    }

    private var productImage: some View {
        AsyncImage(url: URL(string: product.imageUrl)) { phase in
            switch phase {
            case .success(let img):
                img.resizable().aspectRatio(contentMode: .fill)
            case .failure, .empty:
                Rectangle().fill(Color.surface2)
                    .overlay(
                        Text("M")
                            .font(.system(size: 80, weight: .black))
                            .foregroundColor(Color.gold.opacity(0.3))
                    )
            @unknown default:
                Rectangle().fill(Color.surface2)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 420)
        .clipped()
    }

    private var productHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            if product.isNew {
                Text("NEW ARRIVAL")
                    .font(.system(size: 9, weight: .bold))
                    .tracking(3)
                    .foregroundColor(.gold)
            }
            Text(product.title)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.white)
            HStack {
                Text(product.formattedPrice)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.gold)
                Spacer()
                Text("CAD")
                    .font(.system(size: 11))
                    .foregroundColor(.textMuted)
            }
        }
    }

    private var sizeSelector: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("SIZE")
                .font(.system(size: 10, weight: .bold))
                .tracking(3)
                .foregroundColor(.textMuted)
            HStack(spacing: 8) {
                ForEach(product.sizes, id: \.self) { size in
                    Button {
                        selectedSize = size
                    } label: {
                        Text(size)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(selectedSize == size ? .nearBlack : .textSecondary)
                            .frame(minWidth: 44, minHeight: 38)
                            .background(selectedSize == size ? Color.gold : Color.surface2)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var colorSelector: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("COLOR")
                .font(.system(size: 10, weight: .bold))
                .tracking(3)
                .foregroundColor(.textMuted)
            HStack(spacing: 8) {
                ForEach(product.colors, id: \.self) { color in
                    Button {
                        selectedColor = color
                    } label: {
                        Text(color)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(selectedColor == color ? .nearBlack : .textSecondary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(selectedColor == color ? Color.gold : Color.surface2)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var addToCartButton: some View {
        Button {
            let sz = selectedSize.isEmpty ? (product.sizes.first ?? "One Size") : selectedSize
            let cl = selectedColor.isEmpty ? (product.colors.first ?? "Black") : selectedColor
            cart.add(product, size: sz, color: cl)
            withAnimation {
                addedToCart = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                withAnimation { addedToCart = false }
            }
        } label: {
            HStack {
                Image(systemName: addedToCart ? "checkmark" : "bag.badge.plus")
                Text(addedToCart ? "ADDED TO BAG" : "ADD TO BAG")
                    .font(.system(size: 13, weight: .bold))
                    .tracking(3)
            }
            .foregroundColor(.nearBlack)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(addedToCart ? Color.green : Color.gold)
            .clipShape(RoundedRectangle(cornerRadius: 4))
        }
        .animation(.easeInOut(duration: 0.3), value: addedToCart)
        .padding(.top, 8)
    }

    private var viewOnWebButton: some View {
        Link(destination: URL(string: product.productUrl) ?? URL(string: "https://melato.ca")!) {
            HStack {
                Image(systemName: "arrow.up.right.square")
                Text("VIEW ON MELATO.CA")
                    .font(.system(size: 11, weight: .bold))
                    .tracking(2)
            }
            .foregroundColor(.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.surface2)
            .clipShape(RoundedRectangle(cornerRadius: 4))
        }
    }

    private var productDetails: some View {
        VStack(alignment: .leading, spacing: 12) {
            Divider().background(Color.surface3)
            Text("DETAILS")
                .font(.system(size: 10, weight: .bold))
                .tracking(3)
                .foregroundColor(.textMuted)
            Text(product.description.isEmpty
                 ? "Premium quality garment from the Melato FW25 collection. Crafted with intention, cut for the bold."
                 : product.description)
                .font(.system(size: 13))
                .foregroundColor(.textSecondary)
                .lineSpacing(4)
        }
        .padding(.top, 8)
    }
}
