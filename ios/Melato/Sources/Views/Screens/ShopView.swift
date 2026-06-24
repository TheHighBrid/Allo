import SwiftUI

struct ShopView: View {
    let cart: CartViewModel
    @State private var selectedCategory = "all"
    @State private var searchText = ""
    @State private var showSearch = false

    private var filteredProducts: [Product] {
        let base = searchText.isEmpty
            ? ShopifyRepo.byCategory(selectedCategory)
            : ShopifyRepo.search(searchText)
        return base
    }

    var body: some View {
        VStack(spacing: 0) {
            if showSearch {
                searchBar
            }
            categoryBar
            ScrollView(showsIndicators: false) {
                if filteredProducts.isEmpty {
                    emptyState
                } else {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(filteredProducts) { product in
                            NavigationLink {
                                ProductDetailView(product: product, cart: cart)
                            } label: {
                                ProductCard(product: product)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(16)
                }
            }
        }
        .background(Color.nearBlack)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("SHOP")
                    .font(.system(size: 14, weight: .bold))
                    .tracking(6)
                    .foregroundColor(.white)
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    withAnimation { showSearch.toggle() }
                    if !showSearch { searchText = "" }
                } label: {
                    Image(systemName: showSearch ? "xmark" : "magnifyingglass")
                        .foregroundColor(.white)
                }
            }
        }
    }

    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.textMuted)
            TextField("Search products…", text: $searchText)
                .foregroundColor(.white)
                .tint(.gold)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(Color.surface2)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private var categoryBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Category.all) { cat in
                    Button {
                        withAnimation { selectedCategory = cat.id }
                    } label: {
                        Text(cat.name.uppercased())
                            .font(.system(size: 10, weight: .bold))
                            .tracking(2)
                            .foregroundColor(selectedCategory == cat.id ? .nearBlack : .textSecondary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(selectedCategory == cat.id ? Color.gold : Color.surface2)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .background(Color.surface1)
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Text("M")
                .font(.system(size: 60, weight: .black))
                .foregroundColor(Color.gold.opacity(0.3))
            Text("No results found")
                .font(.system(size: 14))
                .foregroundColor(.textMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 80)
    }
}
