# Extraction selector syntax

This project uses `CssSelectorExtractor` to extract fields from a JSoup `Document`.

Supported selector-spec prefixes (inside `selectors` map values):

## 1) Legacy (text)
- `"<css selector>"`
  - extracts `document.selectFirst(selector).text()`

## 2) Social links / arbitrary href
- `href@css(<css selector>)`
  - extracts `href` attribute from **all** matches
  - joins multiple values with `, ` and dedupes (case-insensitive)

## 3) Phone
- `tel@css(<css selector>)`
  - extracts from matched elements' `href`
  - supports `tel:` links; returns normalized phone digits (best-effort)

## 4) Email
- `mailto@css(<css selector>)`
  - extracts from matched elements' `href`
  - supports `mailto:` links; strips query params

## 5) Address container
- `address@css(<css selector>)`
  - extracts an address from the container by joining semantic child elements
  - returns a single Excel-friendly string

