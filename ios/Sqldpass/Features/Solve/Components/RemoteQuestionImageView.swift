import Foundation
import SwiftUI
import UIKit

/// Reusable renderer for question image sources.
struct RemoteQuestionImageView: View {
    let src: String
    let alt: String?
    let minHeight: CGFloat

    init(src: String, alt: String? = nil, minHeight: CGFloat = 140) {
        self.src = src
        self.alt = alt
        self.minHeight = minHeight
    }

    var body: some View {
        content
            .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .center)
            .padding(Spacing.sm)
            .background(Color.appSurface)
            .clipShape(RoundedRectangle(cornerRadius: Radius.md))
            .overlay(
                RoundedRectangle(cornerRadius: Radius.md)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .accessibilityLabel(alt ?? "Question image")
    }

    @ViewBuilder
    private var content: some View {
        switch QuestionImageSource.parse(src) {
        case .remote(let url):
            AsyncImage(url: url) { phase in
                switch phase {
                case .empty:
                    placeholder
                case .success(let image):
                    fittedImage(image)
                case .failure:
                    failure
                @unknown default:
                    failure
                }
            }
        case .raster(let image):
            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity, alignment: .center)
        case .svgXML(let xml):
            InlineSVGView(svgXML: xml, minHeight: minHeight)
        case .unsupported:
            failure
        }
    }

    private var placeholder: some View {
        Text("이미지를 불러오는 중입니다")
            .font(AppType.footnote)
            .foregroundStyle(Color.appTextSubtle)
            .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .center)
    }

    private var failure: some View {
        VStack(spacing: Spacing.xs) {
            Text("이미지를 불러올 수 없습니다")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)

            if let alt, !alt.isEmpty {
                Text(alt)
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .center)
    }

    private func fittedImage(_ image: Image) -> some View {
        image
            .resizable()
            .scaledToFit()
            .frame(maxWidth: .infinity, alignment: .center)
    }
}

private enum QuestionImageSource {
    case remote(URL)
    case raster(UIImage)
    case svgXML(String)
    case unsupported

    static func parse(_ rawValue: String) -> QuestionImageSource {
        let trimmed = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return .unsupported }

        if looksLikeSVGXML(trimmed) {
            return .svgXML(trimmed)
        }

        if let dataImage = parseDataImage(trimmed) {
            return dataImage
        }

        if let url = URL(string: trimmed),
           let scheme = url.scheme?.lowercased(),
           scheme == "http" || scheme == "https" {
            return .remote(url)
        }

        return .unsupported
    }

    private static func looksLikeSVGXML(_ value: String) -> Bool {
        let lowercased = value.lowercased()
        return lowercased.hasPrefix("<svg") || (lowercased.hasPrefix("<?xml") && lowercased.contains("<svg"))
    }

    private static func parseDataImage(_ value: String) -> QuestionImageSource? {
        guard
            value.lowercased().hasPrefix("data:image/"),
            let commaIndex = value.firstIndex(of: ",")
        else {
            return nil
        }

        let header = value[..<commaIndex].lowercased()
        let payload = String(value[value.index(after: commaIndex)...])

        if header.contains("svg+xml") {
            guard let xml = decodePayload(payload, isBase64: header.contains(";base64")) else {
                return .unsupported
            }
            return .svgXML(xml)
        }

        guard
            header.contains(";base64"),
            let data = Data(base64Encoded: sanitizedBase64(payload), options: .ignoreUnknownCharacters),
            let image = UIImage(data: data)
        else {
            return .unsupported
        }

        return .raster(image)
    }

    private static func decodePayload(_ payload: String, isBase64: Bool) -> String? {
        if isBase64 {
            guard let data = Data(base64Encoded: sanitizedBase64(payload), options: .ignoreUnknownCharacters) else {
                return nil
            }
            return String(data: data, encoding: .utf8)
        }

        return payload.removingPercentEncoding ?? payload
    }

    private static func sanitizedBase64(_ value: String) -> String {
        (value.removingPercentEncoding ?? value)
            .replacingOccurrences(of: "\n", with: "")
            .replacingOccurrences(of: "\r", with: "")
            .replacingOccurrences(of: "\t", with: "")
            .replacingOccurrences(of: " ", with: "")
    }
}
