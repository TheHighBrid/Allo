import SwiftUI

extension Color {
    static let nearBlack    = Color(red: 10/255,  green: 10/255,  blue: 10/255)
    static let surface1     = Color(red: 20/255,  green: 20/255,  blue: 20/255)
    static let surface2     = Color(red: 30/255,  green: 30/255,  blue: 30/255)
    static let surface3     = Color(red: 42/255,  green: 42/255,  blue: 42/255)
    static let gold         = Color(red: 201/255, green: 168/255, blue: 76/255)
    static let textPrimary  = Color.white
    static let textSecondary= Color(white: 0.67)
    static let textMuted    = Color(white: 0.40)
}

struct MelatoFont {
    static let display      = Font.system(size: 32, weight: .black, design: .default)
    static let headline     = Font.system(size: 22, weight: .bold)
    static let title        = Font.system(size: 17, weight: .semibold)
    static let body         = Font.system(size: 14, weight: .regular)
    static let caption      = Font.system(size: 12, weight: .regular)
    static let label        = Font.system(size: 11, weight: .semibold)
}
