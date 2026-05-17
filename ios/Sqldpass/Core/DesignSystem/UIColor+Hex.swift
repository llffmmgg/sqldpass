import UIKit

extension UIColor {
    convenience init?(hex: String) {
        var value = hex.uppercased().trimmingCharacters(in: .whitespacesAndNewlines)
        if value.hasPrefix("#") { value.removeFirst() }
        guard let rgb = UInt32(value, radix: 16) else { return nil }

        let r, g, b, a: CGFloat
        if value.count == 8 {
            r = CGFloat((rgb >> 24) & 0xFF) / 255
            g = CGFloat((rgb >> 16) & 0xFF) / 255
            b = CGFloat((rgb >> 8) & 0xFF) / 255
            a = CGFloat(rgb & 0xFF) / 255
        } else {
            r = CGFloat((rgb >> 16) & 0xFF) / 255
            g = CGFloat((rgb >> 8) & 0xFF) / 255
            b = CGFloat(rgb & 0xFF) / 255
            a = 1
        }
        self.init(red: r, green: g, blue: b, alpha: a)
    }
}
