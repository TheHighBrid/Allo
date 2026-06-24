import SwiftUI

struct FaqView: View {
    @State private var expandedId: String? = nil

    private let faqs: [FaqItem] = [
        FaqItem(id: "sizing", question: "How does Melato sizing work?",
                answer: "Melato pieces are cut with a structured, tailored silhouette. We recommend sizing up if you prefer a relaxed fit. Our size guide is available on each product page at melato.ca."),
        FaqItem(id: "shipping", question: "Do you ship internationally?",
                answer: "Yes. Melato ships worldwide. Canadian orders typically arrive within 5–7 business days. International shipping takes 10–21 business days depending on location."),
        FaqItem(id: "returns", question: "What is your return policy?",
                answer: "We accept returns within 14 days of delivery. Items must be unworn, unwashed, and in original packaging. Sale items and accessories are final sale. Visit melato.ca/policies/refund-policy for full details."),
        FaqItem(id: "payment", question: "What payment methods do you accept?",
                answer: "We accept Visa, Mastercard, American Express, Apple Pay, Google Pay, and Shop Pay. All transactions are processed securely."),
        FaqItem(id: "authenticity", question: "Are your products authentic?",
                answer: "Every Melato piece is designed and produced by the Melato brand. We do not wholesale or distribute through third parties — purchase only at melato.ca or through this official app."),
        FaqItem(id: "care", question: "How do I care for my Melato pieces?",
                answer: "Care instructions vary by garment. Most pieces should be cold-washed inside-out and hung to dry. Always check the care label inside your garment."),
        FaqItem(id: "contact", question: "How do I contact Melato?",
                answer: "Reach us at info@melato.ca. We aim to respond within 24–48 hours on business days."),
        FaqItem(id: "collabs", question: "Do you do collaborations or custom orders?",
                answer: "We are selective with partnerships. If you are a brand or artist interested in collaborating, reach out to info@melato.ca with your proposal.")
    ]

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 1) {
                ForEach(faqs) { faq in
                    FaqRow(
                        item: faq,
                        isExpanded: expandedId == faq.id
                    ) {
                        withAnimation(.easeInOut(duration: 0.25)) {
                            expandedId = expandedId == faq.id ? nil : faq.id
                        }
                    }
                }
            }
            .padding(.vertical, 8)
        }
        .background(Color.nearBlack)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("FAQ")
                    .font(.system(size: 14, weight: .bold))
                    .tracking(6)
                    .foregroundColor(.white)
            }
        }
    }
}

struct FaqRow: View {
    let item: FaqItem
    let isExpanded: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text(item.question)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.leading)
                    Spacer()
                    Image(systemName: isExpanded ? "minus" : "plus")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.gold)
                }
                .padding(16)
                if isExpanded {
                    Text(item.answer)
                        .font(.system(size: 13))
                        .foregroundColor(.textSecondary)
                        .lineSpacing(4)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 16)
                }
            }
            .background(Color.surface1)
        }
        .buttonStyle(.plain)
    }
}
