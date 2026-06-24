import SwiftUI

struct ProductCard: View {
    let product: Product
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            AsyncImage(url: URL(string: product.imageUrl)) { phase in
                switch phase {
                case .success(let img):
                    img.resizable().aspectRatio(contentMode: .fill)
                case .failure, .empty:
                    Rectangle().fill(Color.surface2)
                        .overlay(Text("M").font(.system(size: 28, weight: .black)).foregroundColor(.gold))
                @unknown default:
                    Rectangle().fill(Color.surface2)
                }
            }
            .frame(height: 200)
            .clipped()

            VStack(alignment: .leading, spacing: 4) {
                if product.isNew {
                    Text("NEW")
                        .font(.system(size: 9, weight: .bold))
                        .tracking(2)
                        .foregroundColor(.gold)
                }
                Text(product.title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.textPrimary)
                    .lineLimit(2)
                Text(product.formattedPrice)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.gold)
            }
            .padding(10)
        }
        .background(Color.surface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
