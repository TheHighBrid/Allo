import SwiftUI

enum Tab: Hashable { case home, shop, cart, profile }

struct AppNavigation: View {
    @StateObject private var cart = CartViewModel()
    @State private var selectedTab: Tab = .home
    @State private var showSplash = true

    var body: some View {
        ZStack {
            if showSplash {
                SplashView { withAnimation { showSplash = false } }
                    .transition(.opacity)
            } else {
                TabView(selection: $selectedTab) {
                    NavigationStack {
                        HomeView(cart: cart, selectedTab: $selectedTab)
                    }
                    .tabItem { Label("Home",  systemImage: "house.fill") }
                    .tag(Tab.home)

                    NavigationStack {
                        ShopView(cart: cart)
                    }
                    .tabItem { Label("Shop",  systemImage: "square.grid.2x2.fill") }
                    .tag(Tab.shop)

                    NavigationStack {
                        CartView(cart: cart)
                    }
                    .tabItem {
                        Label {
                            Text("Cart")
                        } icon: {
                            CartTabIcon(count: cart.count)
                        }
                    }
                    .tag(Tab.cart)

                    NavigationStack {
                        ProfileView()
                    }
                    .tabItem { Label("Profile", systemImage: "person.fill") }
                    .tag(Tab.profile)
                }
                .tint(.gold)
                .onAppear { configureTabBarAppearance() }
            }
        }
        .preferredColorScheme(.dark)
    }

    private func configureTabBarAppearance() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(Color.surface1)
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }
}

struct CartTabIcon: View {
    let count: Int
    var body: some View {
        ZStack(alignment: .topTrailing) {
            Image(systemName: "bag.fill")
            if count > 0 {
                Text("\(min(count, 99))")
                    .font(.system(size: 8, weight: .bold))
                    .foregroundColor(.black)
                    .frame(minWidth: 14, minHeight: 14)
                    .background(Color.gold)
                    .clipShape(Circle())
                    .offset(x: 6, y: -6)
            }
        }
    }
}
