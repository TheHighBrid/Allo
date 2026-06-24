import SwiftUI

struct SplashView: View {
    let onDone: () -> Void
    @State private var wordmarkOpacity = 0.0
    @State private var taglineOpacity  = 0.0
    @State private var subOpacity      = 0.0

    var body: some View {
        ZStack {
            Color.nearBlack.ignoresSafeArea()
            VStack(spacing: 10) {
                Text("MELATO")
                    .font(.system(size: 40, weight: .black))
                    .tracking(12)
                    .foregroundColor(.white)
                    .opacity(wordmarkOpacity)
                Text("COMME NEVER CHANGE")
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(4)
                    .foregroundColor(.gold)
                    .opacity(taglineOpacity)
                Text("Cut with intent.")
                    .font(.system(size: 12))
                    .foregroundColor(.textMuted)
                    .opacity(subOpacity)
            }
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 0.9)) { wordmarkOpacity = 1 }
            withAnimation(.easeInOut(duration: 0.7).delay(1.0)) { taglineOpacity = 1 }
            withAnimation(.easeInOut(duration: 0.6).delay(1.1)) { subOpacity = 1 }
            withAnimation(.easeInOut(duration: 0.5).delay(2.2)) { wordmarkOpacity = 0 }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.7) { onDone() }
        }
    }
}
