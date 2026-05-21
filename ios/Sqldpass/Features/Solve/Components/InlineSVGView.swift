import Foundation
import SwiftUI
import SVGKit

/// Renders inline `<svg>...</svg>` blocks with deterministic SwiftUI sizing.
struct InlineSVGView: View {
    let svgXML: String
    let minHeight: CGFloat

    init(svgXML: String, minHeight: CGFloat = 160) {
        self.svgXML = svgXML
        self.minHeight = minHeight
    }

    var body: some View {
        let image = makeImage()
        InlineSVGRepresentable(image: image)
            .aspectRatio(SVGLayout.aspectRatio(svgXML: svgXML, image: image), contentMode: .fit)
            .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .center)
    }

    private func makeImage() -> SVGKImage? {
        guard let data = svgXML.data(using: .utf8) else { return nil }
        return SVGKImage(data: data)
    }
}

private struct InlineSVGRepresentable: UIViewRepresentable {
    let image: SVGKImage?

    func makeUIView(context: Context) -> SVGKFastImageView {
        let view = SVGKFastImageView(svgkImage: image) ?? SVGKFastImageView(frame: .zero)
        view.contentMode = .scaleAspectFit
        view.backgroundColor = .clear
        view.setContentHuggingPriority(.defaultLow, for: .horizontal)
        view.setContentCompressionResistancePriority(.required, for: .vertical)
        return view
    }

    func updateUIView(_ uiView: SVGKFastImageView, context: Context) {
        uiView.image = image
        uiView.contentMode = .scaleAspectFit
    }
}

private enum SVGLayout {
    private static let fallbackAspectRatio: CGFloat = 16.0 / 9.0

    static func aspectRatio(svgXML: String, image: SVGKImage?) -> CGFloat {
        if let size = image?.size, size.width > 0, size.height > 0 {
            return size.width / size.height
        }

        if let ratio = aspectRatioFromRootAttributes(svgXML) {
            return ratio
        }

        return fallbackAspectRatio
    }

    private static func aspectRatioFromRootAttributes(_ svgXML: String) -> CGFloat? {
        guard let data = svgXML.data(using: .utf8) else { return nil }

        let delegate = SVGRootAttributeParser()
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        _ = parser.parse()

        if let viewBox = delegate.attributes["viewBox"] ?? delegate.attributes["viewbox"],
           let ratio = aspectRatioFromViewBox(viewBox) {
            return ratio
        }

        guard
            let widthValue = delegate.attributes["width"],
            let heightValue = delegate.attributes["height"],
            let width = dimension(from: widthValue),
            let height = dimension(from: heightValue),
            height > 0
        else {
            return nil
        }

        return width / height
    }

    private static func aspectRatioFromViewBox(_ viewBox: String) -> CGFloat? {
        let parts = viewBox
            .split { $0 == " " || $0 == "," || $0 == "\n" || $0 == "\t" }
            .compactMap { Double(String($0)) }

        guard parts.count == 4, parts[2] > 0, parts[3] > 0 else { return nil }
        return CGFloat(parts[2] / parts[3])
    }

    private static func dimension(from rawValue: String) -> CGFloat? {
        let value = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.hasSuffix("%") else { return nil }

        let scanner = Scanner(string: value)
        var number = 0.0
        guard scanner.scanDouble(&number), number > 0 else { return nil }
        return CGFloat(number)
    }
}

private final class SVGRootAttributeParser: NSObject, XMLParserDelegate {
    private(set) var attributes: [String: String] = [:]

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String]
    ) {
        guard elementName.lowercased() == "svg" else { return }
        attributes = attributeDict
        parser.abortParsing()
    }
}
