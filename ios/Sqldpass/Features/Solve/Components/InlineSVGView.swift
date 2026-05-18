import SwiftUI
import SVGKit

/// 본문에 박힌 inline `<svg>...</svg>` 를 SVGKit 로 렌더.
///
/// Android `InlineSvgView` (Coil + SvgDecoder) 와 동치.
/// `SVGKFastImageView` 는 CALayer 기반이라 큰 SVG 도 가볍게 렌더.
struct InlineSVGView: UIViewRepresentable {
    let svgXML: String

    func makeUIView(context: Context) -> SVGKFastImageView {
        let view = SVGKFastImageView(svgkImage: makeImage()) ?? SVGKFastImageView(frame: .zero)
        view.contentMode = .scaleAspectFit
        view.translatesAutoresizingMaskIntoConstraints = false
        view.setContentHuggingPriority(.required, for: .vertical)
        view.setContentCompressionResistancePriority(.required, for: .vertical)
        return view
    }

    func updateUIView(_ uiView: SVGKFastImageView, context: Context) {
        if let image = makeImage() {
            uiView.image = image
        }
    }

    private func makeImage() -> SVGKImage? {
        guard let data = svgXML.data(using: .utf8) else { return nil }
        return SVGKImage(data: data)
    }
}
