import SwiftUI

struct ProfileView: View {
    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                brandHeader
                VStack(spacing: 1) {
                    sectionHeader("SHOP")
                    linkRow(icon: "globe", label: "melato.ca", url: "https://melato.ca")
                    linkRow(icon: "cart", label: "Full Collection", url: "https://melato.ca/collections/all")

                    sectionHeader("CONTACT")
                    linkRow(icon: "envelope", label: "info@melato.ca", url: "mailto:info@melato.ca")
                    linkRow(icon: "phone", label: "Ottawa, Canada", url: "https://melato.ca")

                    sectionHeader("SOCIAL")
                    linkRow(icon: "camera", label: "Instagram", url: "https://instagram.com/melato.ca")
                    linkRow(icon: "play.rectangle", label: "TikTok", url: "https://tiktok.com/@melato.ca")

                    sectionHeader("SUPPORT")
                    NavigationLink {
                        FaqView()
                    } label: {
                        rowContent(icon: "questionmark.circle", label: "FAQ")
                    }
                    .buttonStyle(.plain)
                    linkRow(icon: "arrow.counterclockwise", label: "Returns & Exchanges", url: "https://melato.ca/policies/refund-policy")
                    linkRow(icon: "shippingbox", label: "Shipping Info", url: "https://melato.ca/policies/shipping-policy")

                    sectionHeader("LEGAL")
                    linkRow(icon: "doc.text", label: "Privacy Policy", url: "https://melato.ca/policies/privacy-policy")
                    linkRow(icon: "doc.text", label: "Terms of Service", url: "https://melato.ca/policies/terms-of-service")
                }
                .padding(.bottom, 40)
            }
        }
        .background(Color.nearBlack)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("MELATO")
                    .font(.system(size: 14, weight: .bold))
                    .tracking(6)
                    .foregroundColor(.white)
            }
        }
    }

    private var brandHeader: some View {
        VStack(spacing: 8) {
            Text("M")
                .font(.system(size: 56, weight: .black))
                .foregroundColor(.gold)
            Text("MELATO")
                .font(.system(size: 12, weight: .black))
                .tracking(8)
                .foregroundColor(.white)
            Text("Ottawa, Canada · melato.ca")
                .font(.system(size: 11))
                .foregroundColor(.textMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .background(Color.surface1)
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 9, weight: .bold))
            .tracking(4)
            .foregroundColor(.textMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 20)
            .padding(.bottom, 4)
    }

    private func linkRow(icon: String, label: String, url: String) -> some View {
        Link(destination: URL(string: url)!) {
            rowContent(icon: icon, label: label)
        }
        .buttonStyle(.plain)
    }

    private func rowContent(icon: String, label: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(.gold)
                .frame(width: 24)
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.white)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 12))
                .foregroundColor(.textMuted)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color.surface1)
    }
}
