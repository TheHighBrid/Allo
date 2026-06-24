import SwiftUI

struct HomeView: View {
    let cart: CartViewModel
    @Binding var selectedTab: Tab
    @State private var showPopup = false
    @State private var currentPopup: PopupEvent? = nil

    private let popups: [PopupEvent] = [
        PopupEvent(id: "fw25", title: "FW25 DROP", subtitle: "The full Fall/Winter 2025 collection is live.", imageName: nil, ctaLabel: "Shop Now"),
        PopupEvent(id: "lookbook", title: "LIVING LOOKBOOK", subtitle: "See how Melato moves in the world.", imageName: nil, ctaLabel: "Explore")
    ]

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                heroBanner
                newArrivalsSection
                collectionsGrid
                featuredSection
                Spacer(minLength: 40)
            }
        }
        .background(Color.nearBlack)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("MELATO")
                    .font(.system(size: 18, weight: .black))
                    .tracking(8)
                    .foregroundColor(.white)
            }
        }
        .onAppear {
            if !showPopup, let first = popups.first {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                    currentPopup = first
                    withAnimation { showPopup = true }
                }
            }
        }
        .overlay {
            if showPopup, let popup = currentPopup {
                PopupOverlay(event: popup) {
                    withAnimation { showPopup = false }
                } onCta: {
                    withAnimation { showPopup = false }
                    if popup.ctaLabel == "Shop Now" { selectedTab = .shop }
                }
            }
        }
    }

    private var heroBanner: some View {
        ZStack(alignment: .bottomLeading) {
            Rectangle()
                .fill(
                    LinearGradient(
                        colors: [Color.surface2, Color.nearBlack],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .frame(height: 380)
                .overlay(
                    Text("M")
                        .font(.system(size: 220, weight: .black))
                        .tracking(-10)
                        .foregroundColor(Color.gold.opacity(0.08))
                        .offset(x: 20, y: -20),
                    alignment: .center
                )
            VStack(alignment: .leading, spacing: 6) {
                Text("COMME NEVER CHANGE")
                    .font(.system(size: 9, weight: .bold))
                    .tracking(4)
                    .foregroundColor(.gold)
                Text("FW25")
                    .font(.system(size: 52, weight: .black))
                    .tracking(4)
                    .foregroundColor(.white)
                Text("Cut with intent.")
                    .font(.system(size: 14))
                    .foregroundColor(.textSecondary)
                Button {
                    selectedTab = .shop
                } label: {
                    Text("SHOP THE COLLECTION")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(3)
                        .foregroundColor(.nearBlack)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Color.gold)
                }
                .padding(.top, 8)
            }
            .padding(24)
        }
    }

    private var newArrivalsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader("NEW ARRIVALS")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(ShopifyRepo.newArrivals()) { product in
                        NavigationLink {
                            ProductDetailView(product: product, cart: cart)
                        } label: {
                            ProductCard(product: product)
                                .frame(width: 180)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
        .padding(.top, 32)
    }

    private var collectionsGrid: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader("COLLECTIONS")
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(Category.all) { cat in
                    if cat.id != "all" {
                        Button {
                            selectedTab = .shop
                        } label: {
                            ZStack {
                                Rectangle()
                                    .fill(Color.surface2)
                                    .frame(height: 120)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                VStack(spacing: 4) {
                                    Text(cat.name.uppercased())
                                        .font(.system(size: 12, weight: .bold))
                                        .tracking(3)
                                        .foregroundColor(.white)
                                    Text("\(ShopifyRepo.byCategory(cat.id).count) pieces")
                                        .font(.system(size: 10))
                                        .foregroundColor(.textMuted)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 32)
    }

    private var featuredSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader("FEATURED")
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(ShopifyRepo.featured()) { product in
                    NavigationLink {
                        ProductDetailView(product: product, cart: cart)
                    } label: {
                        ProductCard(product: product)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
        }
        .padding(.top, 32)
    }

    private func sectionHeader(_ title: String) -> some View {
        HStack {
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .tracking(4)
                .foregroundColor(.textMuted)
            Rectangle()
                .fill(Color.surface3)
                .frame(height: 1)
        }
        .padding(.horizontal, 16)
    }
}

struct PopupOverlay: View {
    let event: PopupEvent
    let onDismiss: () -> Void
    let onCta: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.7).ignoresSafeArea()
                .onTapGesture { onDismiss() }
            VStack(spacing: 0) {
                Rectangle()
                    .fill(Color.surface2)
                    .frame(height: 160)
                    .overlay(
                        Text("M")
                            .font(.system(size: 100, weight: .black))
                            .foregroundColor(Color.gold.opacity(0.15))
                    )
                VStack(spacing: 8) {
                    Text(event.title)
                        .font(.system(size: 9, weight: .bold))
                        .tracking(4)
                        .foregroundColor(.gold)
                    Text(event.subtitle)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                    Button(action: onCta) {
                        Text(event.ctaLabel.uppercased())
                            .font(.system(size: 11, weight: .bold))
                            .tracking(3)
                            .foregroundColor(.nearBlack)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.gold)
                    }
                    .padding(.top, 8)
                    Button(action: onDismiss) {
                        Text("Maybe Later")
                            .font(.system(size: 12))
                            .foregroundColor(.textMuted)
                    }
                    .padding(.top, 4)
                }
                .padding(20)
                .background(Color.surface1)
            }
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(32)
        }
    }
}
