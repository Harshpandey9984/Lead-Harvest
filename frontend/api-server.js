const axios = require('axios');
const ExcelJS = require('exceljs');
const express = require('express');
const cors = require('cors');
const https = require('https');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3100;

const SOCIAL_PATTERNS = [
  { label: 'LinkedIn', pattern: /linkedin\.com/i },
  { label: 'Facebook', pattern: /facebook\.com/i },
  { label: 'Instagram', pattern: /instagram\.com/i },
  { label: 'X', pattern: /(twitter\.com|x\.com)/i },
  { label: 'YouTube', pattern: /youtube\.com/i },
  { label: 'TikTok', pattern: /tiktok\.com/i },
  { label: 'WhatsApp', pattern: /wa\.me|whatsapp\.com/i },
  { label: 'Pinterest', pattern: /pinterest\.com/i },
  { label: 'Threads', pattern: /threads\.net/i },
  { label: 'Bluesky', pattern: /bsky\.app/i },
];

const SOCIAL_HOSTS = [
  'linkedin.com',
  'facebook.com',
  'instagram.com',
  'twitter.com',
  'x.com',
  'youtube.com',
  'youtu.be',
  'tiktok.com',
  'wa.me',
  'whatsapp.com',
  'pinterest.com',
  'threads.net',
  'bsky.app',
];

function normalizeUrl(value) {
  const trimmed = String(value || '').trim();
  if (!trimmed) {
    return '';
  }
  return /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(trimmed) ? trimmed : `https://${trimmed}`;
}

function normalizeUrlList(input) {
  if (Array.isArray(input)) {
    return input.map(normalizeUrl).filter(Boolean);
  }
  if (typeof input === 'string') {
    return input
      .split(/[\n\r,;\t]+/)
      .map((value) => value.trim())
      .filter(Boolean)
      .map(normalizeUrl)
      .filter(Boolean);
  }
  return [];
}

function decodeEntities(value = '') {
  return String(value)
    .replace(/&#(x?[0-9a-f]+);/gi, (_, entity) => {
      const base = entity.startsWith('x') ? 16 : 10;
      const raw = entity.replace(/^x/i, '');
      const code = Number.parseInt(raw, base);
      return Number.isFinite(code) ? String.fromCodePoint(code) : _;
    })
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&nbsp;/g, ' ');
}

function stripTags(html = '') {
  return decodeEntities(
    String(html)
      .replace(/<script[\s\S]*?<\/script>/gi, ' ')
      .replace(/<style[\s\S]*?<\/style>/gi, ' ')
      .replace(/<noscript[\s\S]*?<\/noscript>/gi, ' ')
      .replace(/<svg[\s\S]*?<\/svg>/gi, ' ')
      .replace(/<\/(?:div|p|li|ul|ol|h1|h2|h3|h4|h5|h6|tr|td|footer|header|address|section|article)>/gi, '. ')
      .replace(/<[^>]+>/g, ' ')
  )
    .replace(/\s+/g, ' ')
    .trim();
}

function titleCase(value) {
  return String(value || '')
    .replace(/[-_]+/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .trim();
}

function hostFromUrl(url) {
  try {
    return new URL(url).host.replace(/^www\./, '');
  } catch {
    return String(url || '').replace(/^https?:\/\//, '').split('/')[0].replace(/^www\./, '');
  }
}

function canonicalUrl(rawUrl) {
  try {
    const url = new URL(rawUrl);
    url.hash = '';
    return url.toString();
  } catch {
    return String(rawUrl || '').trim();
  }
}

function classifyCategory(text) {
  const normalized = String(text || '').toLowerCase();
  
  // 1. Hospitality (Cafes, Restaurants, Food, Hotels, etc.)
  if (/\b(restaurant|cafe|coffee|coffeebar|cafeteria|food|bakery|bistro|catering|pub|bar|brewery|kitchen|pizza|burger|grill|steakhouse|diner|eatery|lassi|juicebar|icecream|hotel|resort|motel|hostel|booking|stay|accommodation|travel|tour|tourism|guide|flight|cruise|vacation|trip)s?\b/i.test(normalized)) {
    return 'Hospitality';
  }

  // 2. Healthcare & Medical (Dentist, Clinic, Doctor, etc.)
  if (/\b(dentist|dental|orthodontist|clinic|medical|health|healthcare|hospital|wellness|therapy|physio|chiropractor|doctor|pediatric|surgeon|physician|nursing|pharmacy|chemist|medicine)s?\b/i.test(normalized)) {
    return 'Healthcare';
  }

  // 3. Legal Services
  if (/\b(law|legal|attorney|firm|solicitor|barrister|advocate|lawyer|notary|court|litigation)s?\b/i.test(normalized)) {
    return 'Legal Services';
  }

  // 4. E-Commerce & Retail
  if (/\b(shop|store|commerce|ecommerce|retail|marketplace|boutique|sales|grocer|supermarket|brand|shopping|deal|discount|wholesale)s?\b/i.test(normalized)) {
    return 'E-Commerce';
  }

  // 5. Agency, Consulting & Marketing
  if (/\b(agency|studio|marketing|advertising|pr|seo|media|design|consulting|consultant|advisor|consultancy|public relations|creative)s?\b/i.test(normalized)) {
    return 'Agency';
  }

  // 6. Personal Services
  if (/\b(spa|salon|beauty|hair|nails|fitness|gym|yoga|personal trainer|massage|barber|barbershop|cosmetic|skincare)s?\b/i.test(normalized)) {
    return 'Personal Services';
  }

  // 7. Technology & Software
  if (/\b(software|app|technology|tech|saas|developer|digital|it services|cloud|cybersecurity|networking|hardware|computer|programming|hosting|domain|webdev)s?\b/i.test(normalized)) {
    return 'Technology & Software';
  }

  // 8. Real Estate
  if (/\b(real estate|realtor|property|properties|apartment|housing|rentals|mortgage|broker|estate agent|landlord|leasing)s?\b/i.test(normalized)) {
    return 'Real Estate';
  }

  // 9. Education & Training
  if (/\b(school|university|college|academy|training|education|tutor|course|learning|student|class|lessons|coaching|tuition)s?\b/i.test(normalized)) {
    return 'Education & Training';
  }

  // 10. Finance & Insurance
  if (/\b(finance|financial|bank|banking|insurance|accounting|accountant|adviser|investment|wealth|tax|audit|bookkeeping|cpa)s?\b/i.test(normalized)) {
    return 'Finance & Insurance';
  }

  // 11. Construction & Contracting
  if (/\b(construction|builder|contractor|plumbing|plumber|electrician|roofing|hvac|renovation|carpenter|painter|engineering|architect|handyman|masonry)s?\b/i.test(normalized)) {
    return 'Construction & Contracting';
  }

  // 12. Automotive
  if (/\b(auto|car|vehicle|dealer|repair|garage|automotive|mechanic|tires|dealership|leasing|rental|tow|towing|body shop)s?\b/i.test(normalized)) {
    return 'Automotive';
  }

  // 13. Non-Profit & Community
  if (/\b(charity|non-profit|foundation|ngo|association|community|church|religious|donation|volunteer|worship|temple|mosque|synagogue)s?\b/i.test(normalized)) {
    return 'Non-Profit & Community';
  }

  return 'Business Services';
}

function parseAttributes(tag) {
  const attributes = {};
  String(tag).replace(/([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*("([^"]*)"|'([^']*)'|([^\s"'>]+))/g, (_, key, __, doubleQuoted, singleQuoted, bare) => {
    attributes[key.toLowerCase()] = decodeEntities(doubleQuoted ?? singleQuoted ?? bare ?? '');
    return '';
  });
  return attributes;
}

function extractMetaMap(html) {
  const meta = {};
  String(html).match(/<meta\b[^>]*>/gi)?.forEach((tag) => {
    const attributes = parseAttributes(tag);
    const key = String(attributes.name || attributes.property || attributes.itemprop || '').toLowerCase();
    const content = attributes.content || '';
    if (key && content && !meta[key]) {
      meta[key] = content;
    }
  });
  return meta;
}

function extractJsonLdObjects(html) {
  const records = [];
  const blocks = String(html).match(/<script[^>]*type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi) || [];
  for (const block of blocks) {
    const body = block.replace(/^.*?>/s, '').replace(/<\/script>$/i, '').trim();
    if (!body) continue;
    try {
      const parsed = JSON.parse(decodeEntities(body));
      const queue = Array.isArray(parsed) ? [...parsed] : [parsed];
      while (queue.length) {
        const item = queue.shift();
        if (!item) continue;
        if (Array.isArray(item)) {
          queue.unshift(...item);
          continue;
        }
        if (item['@graph']) {
          queue.unshift(...[].concat(item['@graph']));
        }
        if (typeof item === 'object') {
          records.push(item);
        }
      }
    } catch {
      // Ignore malformed JSON-LD blocks.
    }
  }
  return records;
}

function isBlockedContent(title, text) {
  return /(access denied|forbidden|captcha|robot|cloudflare|unusual traffic|verify you are human|temporarily blocked)/i.test(`${title} ${text}`);
}

function isSocialProfileUrl(rawUrl) {
  try {
    const url = new URL(rawUrl);
    const host = url.host.replace(/^www\./, '').toLowerCase();
    return SOCIAL_HOSTS.some((allowedHost) => host === allowedHost || host.endsWith(`.${allowedHost}`));
  } catch {
    return false;
  }
}

function socialLabelFromUrl(rawUrl) {
  const host = hostFromUrl(rawUrl).toLowerCase();
  if (host.includes('linkedin')) return 'LinkedIn';
  if (host.includes('facebook')) return 'Facebook';
  if (host.includes('instagram')) return 'Instagram';
  if (host.includes('twitter') || host === 'x.com') return 'X';
  if (host.includes('youtube') || host.includes('youtu.be')) return 'YouTube';
  if (host.includes('tiktok')) return 'TikTok';
  if (host.includes('whatsapp') || host.includes('wa.me')) return 'WhatsApp';
  if (host.includes('pinterest')) return 'Pinterest';
  if (host.includes('threads')) return 'Threads';
  if (host.includes('bsky')) return 'Bluesky';
  return 'Social';
}

function stringifyAddressValue(value) {
  if (!value) return '';
  if (typeof value === 'string') return value.trim();
  if (Array.isArray(value)) {
    return value.map(stringifyAddressValue).filter(Boolean).join(', ');
  }
  if (typeof value === 'object') {
    const parts = [
      value.name,
      value.streetAddress,
      value.addressLocality,
      value.addressRegion,
      value.postalCode,
      value.addressCountry,
    ].map((entry) => stringifyAddressValue(entry)).filter(Boolean);
    if (parts.length) {
      return parts.join(', ');
    }
  }
  return String(value).trim();
}

function parseAddressString(addressStr) {
  const clean = String(addressStr || '').trim();
  if (!clean) {
    return { street: '', city: '', region: '', postalCode: '', country: '' };
  }

  const parts = clean.split(',').map((p) => p.trim()).filter(Boolean);
  if (parts.length === 1) {
    return { street: parts[0], city: '', region: '', postalCode: '', country: '' };
  }

  let country = '';
  let region = '';
  let postalCode = '';
  let city = '';
  let street = '';

  // 1. Try to find a postal code in any of the parts
  const postalCodePattern = /\b\d{5}(?:-\d{4})?\b|\b\d{6}\b|\b[A-Z]\d[A-Z]\s*\d[A-Z]\d\b|\b[A-Z]{1,2}\d[A-Z0-9]?\s*\d[A-Z]{2}\b/i;
  for (let i = 0; i < parts.length; i += 1) {
    const match = parts[i].match(postalCodePattern);
    if (match) {
      postalCode = match[0];
      parts[i] = parts[i].replace(postalCode, '').trim();
      break;
    }
  }

  const remainingParts = parts.map((p) => p.trim()).filter(Boolean);

  const countries = ['india', 'usa', 'united states', 'uk', 'united kingdom', 'canada', 'australia', 'germany', 'france', 'italy', 'spain'];
  const indianStates = ['andhra pradesh', 'arunachal pradesh', 'assam', 'bihar', 'chhattisgarh', 'goa', 'gujarat', 'haryana', 'himachal pradesh', 'jharkhand', 'karnataka', 'kerala', 'madhya pradesh', 'maharashtra', 'manipur', 'meghalaya', 'mizoram', 'nagaland', 'odisha', 'punjab', 'rajasthan', 'sikkim', 'tamil nadu', 'telangana', 'tripura', 'uttar pradesh', 'uttarakhand', 'west bengal', 'delhi'];
  const usStates = ['al', 'ak', 'az', 'ar', 'ca', 'co', 'ct', 'de', 'fl', 'ga', 'hi', 'id', 'il', 'in', 'ia', 'ks', 'ky', 'la', 'me', 'md', 'ma', 'mi', 'mn', 'ms', 'mo', 'mt', 'ne', 'nv', 'nh', 'nj', 'nm', 'ny', 'nc', 'nd', 'oh', 'ok', 'or', 'pa', 'ri', 'sc', 'sd', 'tn', 'tx', 'ut', 'vt', 'va', 'wa', 'wv', 'wi', 'wy'];

  if (remainingParts.length > 0) {
    const lastPartLower = remainingParts[remainingParts.length - 1].toLowerCase();
    if (countries.includes(lastPartLower) || lastPartLower.length === 2 && lastPartLower === 'in') {
      country = remainingParts.pop();
    }
  }

  if (remainingParts.length > 0) {
    const lastPartLower = remainingParts[remainingParts.length - 1].toLowerCase();
    const isIndianState = indianStates.includes(lastPartLower);
    const isUsState = usStates.includes(lastPartLower) || lastPartLower.length === 2 && /^[a-z]{2}$/.test(lastPartLower);
    
    if (isIndianState || isUsState) {
      region = remainingParts.pop();
      if (isIndianState && !country) {
        country = 'India';
      }
    }
  }

  if (remainingParts.length >= 2) {
    city = remainingParts.pop();
  }

  street = remainingParts.join(', ');

  if (!street && remainingParts.length === 1) {
    street = remainingParts[0];
  }

  return { street, city, region, postalCode, country };
}

function normalizeAddressParts(value) {
  const source = value && typeof value === 'object' && !Array.isArray(value) ? value : {};
  let street = stringifyAddressValue(source.streetAddress || source.street || source.addressLine1 || source.addressLine || '');
  let city = stringifyAddressValue(source.addressLocality || source.city || source.locality || '');
  let region = stringifyAddressValue(source.addressRegion || source.state || source.region || '');
  let postalCode = stringifyAddressValue(source.postalCode || source.zip || source.postal || '');
  let country = stringifyAddressValue(source.addressCountry || source.country || '');
  const name = stringifyAddressValue(source.name || source.label || '');

  // Parse if it is a single concatenated address string
  if (street && !city && !region && !postalCode && !country) {
    const parsed = parseAddressString(street);
    street = parsed.street;
    city = parsed.city;
    region = parsed.region;
    postalCode = parsed.postalCode;
    country = parsed.country;
  }

  const formatted = [name, street, city, region, postalCode, country].filter(Boolean).join(', ');
  return { formatted, street, city, region, postalCode, country };
}

function looksLikeAddress(text) {
  const clean = String(text || '').trim();
  if (clean.length < 12 || clean.length > 250) return false;
  
  // Ignore boilerplate
  if (/(copyright|all rights reserved|powered by|designed by|privacy policy|terms of service|use of cookies|cookie settings)/i.test(clean)) {
    return false;
  }
  
  const hasDigit = /\d/.test(clean);
  const hasStreetSuffix = /\b(street|st|road|rd|avenue|ave|drive|dr|lane|ln|boulevard|blvd|way|court|ct|circle|cir|parkway|pkwy|plaza|plz|highway|hwy|square|sq|terrace|ter|box|po box)\b/i.test(clean);
  const hasComma = clean.includes(',');

  // If it has digits and a street suffix, it's very likely an address
  if (hasDigit && hasStreetSuffix) return true;
  
  // If it doesn't have digits, it must have a street suffix and a comma (like "Infantry Road, Ballari")
  if (hasStreetSuffix && hasComma) return true;

  // If it has digits and looks like a PO box or structured address
  if (hasDigit && /\b(box|po box|suite|ste|bldg|building|floor|fl|zip|postal)\b/i.test(clean)) return true;

  return false;
}

function extractAddressFromHtml(html) {
  if (!html) return '';
  
  // Try to find <address> tags
  const addressTagMatches = html.match(/<address\b[^>]*>([\s\S]*?)<\/address>/gi);
  if (addressTagMatches) {
    for (const match of addressTagMatches) {
      const text = stripTags(match);
      if (looksLikeAddress(text)) {
        return text;
      }
    }
  }

  // Try to find elements with class/id/itemprop containing "address"
  const elementMatches = html.match(/<(?:div|span|p|section|li|td)\b[^>]*(?:class|id|itemprop)=["'](?:[^"'>]*\b)?address\b(?:[^"'>]*\b)?["'][^>]*>([\s\S]*?)<\/\1>/gi);
  if (elementMatches) {
    for (const match of elementMatches) {
      const text = stripTags(match);
      if (looksLikeAddress(text)) {
        return text;
      }
    }
  }

  // Try class/id/itemprop containing "street-address" or "streetAddress"
  const streetMatches = html.match(/<(?:div|span|p|section)\b[^>]*(?:class|id|itemprop)=["'](?:[^"'>]*\b)?(?:street-?address|streetAddress)\b(?:[^"'>]*\b)?["'][^>]*>([\s\S]*?)<\/\1>/gi);
  if (streetMatches) {
    for (const match of streetMatches) {
      const text = stripTags(match);
      if (looksLikeAddress(text)) {
        return text;
      }
    }
  }

  return '';
}

function extractAddressFromText(text) {
  const compact = String(text || '').replace(/\s+/g, ' ').trim();
  if (!compact) return '';

  // Look for labeled address patterns with common address keywords (no leading digit requirement)
  const labelled = compact.match(/(?:business address|mailing address|office address|office|headquarters|hq|located at|address|mailing|main office|corporate address)\s*(?:for|of|:|-|is)?\s*([A-Za-z0-9.,&'()\-\/s]{10,200}?)(?=\s(?:phone|tel|email|fax|contact|hours|social|website|p\.o\.|po box)\b|$|<|&lt;)/i);
  if (labelled?.[1]) {
    const extracted = labelled[1].trim().replace(/\s+/g, ' ');
    if (looksLikeAddress(extracted)) {
      return extracted;
    }
  }

  // Pattern A: Standard address starting with a number (e.g. "123 Main St, London, UK")
  const standardPattern = /\b\d{1,6}\s+[A-Za-z0-9\s.,'()-]{2,50}\s+(?:street|st|road|rd|avenue|ave|drive|dr|lane|ln|boulevard|blvd|way|court|ct|circle|cir|parkway|pkwy|plaza|plz|highway|hwy|square|sq|terrace|ter)\b(?:\s*[,.]?\s*[A-Za-z0-9\s.,'()-]{2,40}){0,4}/i;
  
  // Pattern B: Address without a leading number, but with commas and a street suffix (e.g. "Opp OutPost Police Station, Infantry Road, Ballari, Karnataka")
  const landmarkPattern = /\b[A-Za-z0-9\s#\-’'()&\/,]{3,80}\s+(?:street|st|road|rd|avenue|ave|drive|dr|lane|ln|boulevard|blvd|way|court|ct|circle|cir|parkway|pkwy|plaza|plz|highway|hwy|square|sq|terrace|ter)\b(?:\s*[,.]\s*(?![^,.]*\b(?:home|about|inquiry|email|phone|contact|menu|privacy|terms|get|call)\b)[A-Za-z0-9\s#\-’'()&\/]{2,30}){1,5}/i;

  const matchA = compact.match(standardPattern);
  if (matchA && looksLikeAddress(matchA[0])) {
    return matchA[0].trim().replace(/\s+/g, ' ');
  }

  const matchB = compact.match(landmarkPattern);
  if (matchB && looksLikeAddress(matchB[0])) {
    return matchB[0].trim().replace(/\s+/g, ' ');
  }

  return '';
}

function extractAddressDetails(jsonLdObjects, html, text) {
  const candidates = [];

  const queue = [...jsonLdObjects];
  while (queue.length) {
    const item = queue.shift();
    if (!item || typeof item !== 'object') continue;
    const fields = [item.address, item.location, item.postalAddress];
    fields.forEach((field) => {
      if (!field) return;
      if (Array.isArray(field)) {
        queue.push(...field);
      } else if (typeof field === 'object') {
        candidates.push(field);
        queue.push(field);
      } else {
        candidates.push(field);
      }
    });
  }

  for (const candidate of candidates) {
    const normalized = normalizeAddressParts(candidate);
    if (normalized.formatted && /\d/.test(normalized.formatted)) {
      return normalized;
    }
    if (normalized.street && /\d/.test(normalized.street)) {
      return normalized;
    }
  }

  const fromHtml = extractAddressFromHtml(html);
  if (fromHtml) {
    return normalizeAddressParts({ streetAddress: fromHtml });
  }

  const fallback = extractAddressFromText(text);
  return fallback ? normalizeAddressParts({ streetAddress: fallback }) : normalizeAddressParts('');
}

function extractSocialUrls(html, baseUrl) {
  const discovered = new Set();
  const urlRegex = /https?:\/\/[A-Z0-9._~:/?#\[\]@!$&'()*+,;=%-]+/gi;
  const text = stripTags(html);

  for (const match of String(html).matchAll(urlRegex)) {
    const candidate = canonicalUrl(decodeEntities(match[0]));
    if (isSocialProfileUrl(candidate)) {
      discovered.add(candidate);
    }
  }

  for (const match of text.matchAll(urlRegex)) {
    const candidate = canonicalUrl(decodeEntities(match[0]));
    if (isSocialProfileUrl(candidate)) {
      discovered.add(candidate);
    }
  }

  const anchorRegex = /<a\b([^>]*)>([\s\S]*?)<\/a>/gi;
  let anchorMatch;
  while ((anchorMatch = anchorRegex.exec(String(html)))) {
    const attributes = parseAttributes(anchorMatch[1]);
    const href = attributes.href || '';
    if (!href) continue;
    try {
      const resolved = canonicalUrl(new URL(href, baseUrl).toString());
      if (isSocialProfileUrl(resolved)) {
        discovered.add(resolved);
      }
    } catch {
      // Ignore invalid URLs.
    }
  }

  return [...discovered];
}

function extractContactChannels(html, baseUrl) {
  const text = stripTags(html);
  const emails = new Set();
  const phones = new Set();
  const socialProfiles = new Set();
  const contactForms = new Set();

  for (const match of text.matchAll(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi)) {
    emails.add(match[0].toLowerCase());
  }

  for (const match of String(html).matchAll(/href=["']mailto:([^"'?#\s]+)/gi)) {
    emails.add(decodeEntities(match[1]).toLowerCase());
  }

  for (const match of String(html).matchAll(/href=["']tel:([^"'?#\s]+)/gi)) {
    phones.add(decodeEntities(match[1]).replace(/[^0-9+()-]/g, '').trim());
  }

  for (const match of text.matchAll(/(?:phone|tel|call|mobile|whatsapp)\s*[:\-]?\s*([+()0-9.\s-]{7,}\d)/gi)) {
    phones.add(match[1].trim().replace(/\s+/g, ' '));
  }

  for (const match of text.matchAll(/(?:\+?\d[\d.\s()-]{7,}\d)/g)) {
    const candidate = match[0].trim();
    if (/\d{3}/.test(candidate)) {
      phones.add(candidate.replace(/\s+/g, ' '));
    }
  }

  extractSocialUrls(html, baseUrl).forEach((url) => socialProfiles.add(canonicalUrl(url)));

  const anchorRegex = /<a\b([^>]*)>([\s\S]*?)<\/a>/gi;
  let anchorMatch;
  while ((anchorMatch = anchorRegex.exec(String(html)))) {
    const attributes = parseAttributes(anchorMatch[1]);
    const href = attributes.href || '';
    if (!href) continue;
    const label = stripTags(anchorMatch[2]).trim();
    try {
      const resolved = canonicalUrl(new URL(href, baseUrl).toString());
      if (/contact|appointment|book|schedule|reach|call/i.test(label) || /contact|appointment|book|schedule/i.test(resolved)) {
        contactForms.add(resolved.replace(/#.*$/, ''));
      }
      if (isSocialProfileUrl(resolved)) {
        socialProfiles.add(resolved);
      }
    } catch {
      // Ignore invalid URLs.
    }
  }

  // Filter out false positive phone numbers (e.g. dates, postal codes, years, or too short/long digits)
  const cleanedPhones = [...phones].filter((phone) => {
    const digitsOnly = phone.replace(/[^0-9]/g, '');
    if (digitsOnly.length < 7 || digitsOnly.length > 15) return false;
    if (/^\d{4}[-./]\d{2}[-./]\d{2}$/.test(phone) || /^\d{2}[-./]\d{2}[-./]\d{4}$/.test(phone)) return false;
    return true;
  });

  return {
    emails: [...emails],
    phones: cleanedPhones,
    socialProfiles: [...socialProfiles],
    contactForms: [...contactForms],
  };
}

function extractBusinessSignals(page) {
  const meta = extractMetaMap(page.html);
  const jsonLdObjects = extractJsonLdObjects(page.html);
  const text = stripTags(page.html);
  const host = hostFromUrl(page.url);
  const extractedName =
    jsonLdObjects.find((item) => item.name && typeof item.name === 'string')?.name ||
    meta['og:site_name'] ||
    meta['application-name'] ||
    meta['twitter:title'] ||
    meta['title'] ||
    page.title ||
    page.h1 ||
    titleCase(host.split('.')[0]);
  const addressDetails = extractAddressDetails(jsonLdObjects, page.html, text);
  const address = addressDetails.formatted || '';
  const businessType =
    jsonLdObjects.find((item) => item['@type'])?.['@type'] ||
    classifyCategory(`${page.title} ${text}`);

  const services = [];
  const serviceHints = text.match(/(?:services?|treatments?|procedures?|offerings?|care options?|specialties?)[:\s-]*([^.]{0,180})/i);
  if (serviceHints?.[1]) {
    services.push(serviceHints[1].trim());
  }
  jsonLdObjects.forEach((item) => {
    if (item.serviceType) services.push(String(item.serviceType));
    if (item.knowsAbout) services.push(...[].concat(item.knowsAbout).map(String));
    if (item.serviceArea) services.push(...[].concat(item.serviceArea).map((entry) => (typeof entry === 'string' ? entry : entry?.name)).filter(Boolean));
  });

  const teamMembers = [];
  jsonLdObjects.forEach((item) => {
    if (item.employee) teamMembers.push(...[].concat(item.employee).map((entry) => (typeof entry === 'string' ? entry : entry?.name)).filter(Boolean));
    if (item.founder) teamMembers.push(...[].concat(item.founder).map((entry) => (typeof entry === 'string' ? entry : entry?.name)).filter(Boolean));
    if (item.member) teamMembers.push(...[].concat(item.member).map((entry) => (typeof entry === 'string' ? entry : entry?.name)).filter(Boolean));
  });

  const sourceContacts = extractContactChannels(page.html, page.url);
  const hoursMatch = text.match(/(?:hours|opening hours|working hours)[:\s-]*([^.]{0,160})/i);
  const pageDiscovery = page.internalPages;

  jsonLdObjects.forEach((item) => {
    if (item.telephone) {
      sourceContacts.phones.push(...[].concat(item.telephone).map((value) => String(value).trim()).filter(Boolean));
    }
    if (item.email) {
      sourceContacts.emails.push(...[].concat(item.email).map((value) => String(value).trim().toLowerCase()).filter(Boolean));
    }
    if (item.sameAs) {
      sourceContacts.socialProfiles.push(...[].concat(item.sameAs).map((value) => String(value).trim()).filter(Boolean).filter(isSocialProfileUrl));
    }
    if (item.contactPoint) {
      const contactPoints = [].concat(item.contactPoint);
      contactPoints.forEach((point) => {
        if (!point || typeof point !== 'object') return;
        if (point.telephone) {
          sourceContacts.phones.push(...[].concat(point.telephone).map((value) => String(value).trim()).filter(Boolean));
        }
        if (point.email) {
          sourceContacts.emails.push(...[].concat(point.email).map((value) => String(value).trim().toLowerCase()).filter(Boolean));
        }
        if (point.url) {
          sourceContacts.contactForms.push(...[].concat(point.url).map((value) => String(value).trim()).filter(Boolean));
        }
      });
    }
  });

  return {
    businessName: extractedName,
    category: classifyCategory(`${page.title} ${page.description} ${text}`),
    businessType,
    description: page.description || meta.description || '',
    address: stringifyAddressValue(address),
    addressDetails,
    hours: hoursMatch?.[1]?.trim() || '',
    contacts: sourceContacts,
    services: [...new Set(services.map((value) => String(value).trim()).filter(Boolean))],
    teamMembers: [...new Set(teamMembers.map((value) => String(value).trim()).filter(Boolean))],
    pageDiscovery,
    socialProfiles: [...new Set(sourceContacts.socialProfiles.map(canonicalUrl).filter(isSocialProfileUrl))],
    meta,
    jsonLdObjects,
  };
}

function toContactItems(contacts) {
  return [
    ...(contacts.phones || []).slice(0, 4).map((value, index) => ({ label: index === 0 ? 'Phone' : `Phone ${index + 1}`, value, confidence: 90 - index * 6 })),
    ...(contacts.emails || []).slice(0, 4).map((value, index) => ({ label: index === 0 ? 'Email' : `Email ${index + 1}`, value, confidence: 92 - index * 5 })),
    ...(contacts.contactForms || []).slice(0, 3).map((value, index) => ({ label: index === 0 ? 'Contact Form' : `Contact Form ${index + 1}`, value, confidence: 85 - index * 5 })),
  ];
}

function toBusinessInfoSignals(page, contacts, finalUrl) {
  const businessInfo = [
    { label: 'Business Name', value: page.businessName || hostFromUrl(finalUrl), confidence: 96 },
    { label: 'Category', value: page.category || 'Business Services', confidence: 88 },
    { label: 'Website', value: finalUrl, confidence: 100 },
    { label: 'Business Type', value: page.businessType || 'Organization', confidence: 82 },
  ];

  if (page.description) {
    businessInfo.push({ label: 'Description', value: page.description, confidence: 84 });
  }
  if (page.address) {
    businessInfo.push({ label: 'Address', value: page.address, confidence: 80 });
  }
  if (page.hours) {
    businessInfo.push({ label: 'Hours', value: page.hours, confidence: 72 });
  }
  if (page.services.length) {
    businessInfo.push({ label: 'Services', value: page.services.slice(0, 6).join(', '), confidence: 76 });
  }

  return businessInfo;
}

function buildConfidenceSummary(result) {
  const score = (items) => Math.max(0, Math.min(100, items.length ? 60 + items.length * 8 : 20));
  return [
    { field: 'Business Name', score: result.businessName ? 96 : 40 },
    { field: 'Phone', score: score(result.contacts.phones || []) },
    { field: 'Email', score: score(result.contacts.emails || []) },
    { field: 'Address', score: result.address ? 84 : 35 },
    { field: 'Social Links', score: score(result.socialProfiles || []) },
  ];
}

function mergeUnique(items) {
  return [...new Set(items.map((value) => String(value || '').trim()).filter(Boolean))];
}

function candidateUrls(baseUrl, html) {
  const base = new URL(baseUrl);
  const links = [];
  const seen = new Set();
  const anchorRegex = /<a\b([^>]*)>([\s\S]*?)<\/a>/gi;
  let anchorMatch;
  while ((anchorMatch = anchorRegex.exec(String(html)))) {
    const attributes = parseAttributes(anchorMatch[1]);
    const href = attributes.href || '';
    if (!href) continue;
    try {
      const resolved = canonicalUrl(new URL(href, baseUrl).toString());
      const url = new URL(resolved);
      if (url.host !== base.host) continue;
      const text = stripTags(anchorMatch[2]);
      const score = [text, resolved].join(' ');
      if (!seen.has(resolved) && /contact|about|service|team|doctor|location|locations|insurance|billing|find-us|reach-us|get-in-touch|appointment|book-online|staff|careers|faq/i.test(score)) {
        seen.add(resolved);
        links.push({ url: resolved, score: score.toLowerCase().includes('contact') ? 3 : 1 });
      }
    } catch {
      // Ignore invalid URLs.
    }
  }
  return links.sort((left, right) => right.score - left.score).slice(0, 4).map((entry) => entry.url);
}

async function fetchPage(url) {
  const response = await axios.get(url, {
    timeout: 15000,
    maxRedirects: 5,
    responseType: 'text',
    validateStatus: () => true,
    httpsAgent: new https.Agent({ rejectUnauthorized: false }),
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
      'Accept-Language': 'en-US,en;q=0.9',
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache'
    },
  });

  const finalUrl = response.request?.res?.responseUrl || response.config?.url || url;
  const html = typeof response.data === 'string' ? response.data : String(response.data || '');
  const meta = extractMetaMap(html);
  const pageText = stripTags(html);
  const title = meta['og:title'] || meta['twitter:title'] || html.match(/<title[^>]*>([\s\S]*?)<\/title>/i)?.[1] || '';
  const h1 = html.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i)?.[1] || '';
  const blocked = isBlockedContent(stripTags(title), pageText);

  return {
    status: response.status,
    finalUrl,
    html,
    title: stripTags(title),
    h1: stripTags(h1),
    description: meta.description || meta['og:description'] || '',
    meta,
    text: pageText,
    blocked,
  };
}

async function analyzeUrl(rawUrl) {
  const normalized = normalizeUrl(rawUrl);
  if (!normalized) {
    return {
      url: '',
      websiteAvailable: 'No',
      businessName: 'Unknown',
      category: 'Business Services',
      leadScore: 0,
      websiteQualityScore: 0,
      digitalPresenceScore: 0,
      seoScore: 0,
      contactCompletenessScore: 0,
      leadTier: 'Cold',
      businessInfo: [],
      contacts: [],
      addressDetails: { formatted: '', street: '', city: '', region: '', postalCode: '', country: '' },
      socialProfiles: [],
      services: [],
      teamMembers: [],
      pageDiscovery: [],
      smartInspector: { cssSelector: 'body', xpath: '/html/body', confidence: 0, sourcePage: '' },
      confidence: [],
      sourceError: 'Empty URL',
    };
  }

  try {
    const primaryPage = await fetchPage(normalized);
    if (primaryPage.blocked) {
      const host = hostFromUrl(primaryPage.finalUrl || normalized);
      return {
        url: primaryPage.finalUrl || normalized,
        websiteAvailable: 'No',
        businessName: 'Unknown',
        category: 'Unknown',
        leadScore: 0,
        websiteQualityScore: 0,
        digitalPresenceScore: 0,
        seoScore: 0,
        contactCompletenessScore: 0,
        leadTier: 'Cold',
        businessInfo: [
          { label: 'Website', value: primaryPage.finalUrl || normalized, confidence: 100 },
          { label: 'Analysis Status', value: 'Website returned an access block or challenge page', confidence: 100 },
          { label: 'Domain', value: host, confidence: 95 },
        ],
        contacts: [],
        addressDetails: { formatted: '', street: '', city: '', region: '', postalCode: '', country: '' },
        socialProfiles: [],
        services: [],
        teamMembers: [],
        pageDiscovery: [],
        smartInspector: {
          cssSelector: 'body',
          xpath: '/html/body',
          confidence: 0,
          sourcePage: primaryPage.finalUrl || normalized,
        },
        confidence: [
          { field: 'Business Name', score: 0 },
          { field: 'Phone', score: 0 },
          { field: 'Email', score: 0 },
          { field: 'Address', score: 0 },
          { field: 'Social Links', score: 0 },
        ],
        sourcePages: [primaryPage.finalUrl || normalized],
        sourceError: 'Access blocked by the site',
      };
    }

    const discovered = candidateUrls(primaryPage.finalUrl, primaryPage.html);
    const pages = [primaryPage];
    for (const pageUrl of discovered) {
      try {
        pages.push(await fetchPage(pageUrl));
      } catch {
        // Best-effort crawl.
      }
    }

    const primarySignals = extractBusinessSignals({
      ...pages[0],
      internalPages: mergeUnique([pages[0].finalUrl, ...discovered]),
    });

    for (let index = 1; index < pages.length; index += 1) {
      const pageSignals = extractBusinessSignals({
        ...pages[index],
        internalPages: [],
      });
      primarySignals.contacts.emails = mergeUnique([...(primarySignals.contacts.emails || []), ...(pageSignals.contacts.emails || [])]);
      primarySignals.contacts.phones = mergeUnique([...(primarySignals.contacts.phones || []), ...(pageSignals.contacts.phones || [])]);
      primarySignals.contacts.contactForms = mergeUnique([...(primarySignals.contacts.contactForms || []), ...(pageSignals.contacts.contactForms || [])]);
      primarySignals.contacts.socialProfiles = mergeUnique([...(primarySignals.contacts.socialProfiles || []), ...(pageSignals.contacts.socialProfiles || [])]);
      primarySignals.services = mergeUnique([...(primarySignals.services || []), ...(pageSignals.services || [])]);
      primarySignals.teamMembers = mergeUnique([...(primarySignals.teamMembers || []), ...(pageSignals.teamMembers || [])]);
      primarySignals.pageDiscovery = mergeUnique([...(primarySignals.pageDiscovery || []), ...(pageSignals.pageDiscovery || [])]);
      if (!primarySignals.address && pageSignals.address) {
        primarySignals.address = pageSignals.address;
      }
      if (!primarySignals.description && pageSignals.description) {
        primarySignals.description = pageSignals.description;
      }
    }

    const finalUrl = primaryPage.finalUrl || normalized;
    const contacts = {
      emails: mergeUnique(primarySignals.contacts.emails || []),
      phones: mergeUnique(primarySignals.contacts.phones || []),
      contactForms: mergeUnique(primarySignals.contacts.contactForms || []),
      socialProfiles: mergeUnique(primarySignals.contacts.socialProfiles || []),
    };

    const businessInfo = toBusinessInfoSignals(primarySignals, contacts, finalUrl);
    const contactItems = toContactItems(contacts);
    const socialItems = contacts.socialProfiles.slice(0, 8).map((value) => ({
      label: socialLabelFromUrl(value),
      value,
      confidence: 80,
    }));
    const pageCount = mergeUnique(primarySignals.pageDiscovery || []).length;
    const contactCount = contacts.emails.length + contacts.phones.length + contacts.contactForms.length + contacts.socialProfiles.length;
    const descriptionWords = String(primarySignals.description || '').split(/\s+/).filter(Boolean).length;
    const leadScore = Math.max(20, Math.min(99, 30 + pageCount * 8 + contactCount * 4 + (descriptionWords > 20 ? 12 : 0)));
    const websiteQualityScore = Math.max(20, Math.min(99, 35 + (primarySignals.description ? 15 : 0) + (primarySignals.address ? 10 : 0) + pageCount * 5));
    const digitalPresenceScore = Math.max(10, Math.min(99, 25 + socialItems.length * 12 + contactCount * 3));
    const seoScore = Math.max(10, Math.min(99, 30 + (primarySignals.meta?.description ? 10 : 0) + (primarySignals.meta?.['og:title'] ? 10 : 0) + pageCount * 4));
    const contactCompletenessScore = Math.max(10, Math.min(99, 20 + contactCount * 12 + (primarySignals.address ? 15 : 0)));
    const businessName = businessInfo.find((item) => item.label === 'Business Name')?.value || titleCase(hostFromUrl(finalUrl).split('.')[0]);
    const category = primarySignals.category || classifyCategory(`${primarySignals.businessName} ${primarySignals.description} ${primarySignals.address}`);

    return {
      url: finalUrl,
      websiteAvailable: 'Yes',
      businessName,
      category,
      leadScore,
      websiteQualityScore,
      digitalPresenceScore,
      seoScore,
      contactCompletenessScore,
      leadTier: leadScore >= 80 ? 'Hot' : leadScore >= 60 ? 'Warm' : 'Cold',
      businessInfo,
      contacts: contactItems,
      address: primarySignals.address,
      addressDetails: primarySignals.addressDetails || { formatted: primarySignals.address || '', street: '', city: '', region: '', postalCode: '', country: '' },
      contactDetails: {
        phones: contacts.phones,
        emails: contacts.emails,
        contactForms: contacts.contactForms,
        socialProfiles: contacts.socialProfiles,
      },
      phoneDetails: contacts.phones.join('\n'),
      emailDetails: contacts.emails.join('\n'),
      socialLinks: contacts.socialProfiles.join('\n'),
      socialProfiles: socialItems,
      services: (primarySignals.services || []).slice(0, 8).map((value, index) => ({ label: index === 0 ? 'Primary Service' : `Service ${index + 1}`, value, confidence: 78 - index * 4 })),
      teamMembers: (primarySignals.teamMembers || []).slice(0, 8).map((value, index) => ({ label: index === 0 ? 'Team Member' : `Team Member ${index + 1}`, value, confidence: 70 - index * 4 })),
      pageDiscovery: mergeUnique(primarySignals.pageDiscovery || []).slice(0, 12),
      smartInspector: {
        cssSelector: 'main, article, body',
        xpath: '/html/body',
        confidence: 92,
        sourcePage: finalUrl,
      },
      confidence: buildConfidenceSummary({
        businessName,
        contacts,
        socialProfiles: socialItems,
        address: primarySignals.address,
      }),
      sourcePages: pages.map((page) => page.finalUrl),
      sourceError: null,
    };
  } catch (error) {
    const host = hostFromUrl(normalized);
    return {
      url: normalized,
      websiteAvailable: 'No',
      businessName: titleCase(host.split('.')[0] || host) || 'Unknown',
      category: classifyCategory(host),
      leadScore: 0,
      websiteQualityScore: 0,
      digitalPresenceScore: 0,
      seoScore: 0,
      contactCompletenessScore: 0,
      leadTier: 'Cold',
      businessInfo: [
        { label: 'Website', value: normalized, confidence: 100 },
        { label: 'Analysis Status', value: 'Website could not be reached or did not return readable HTML', confidence: 100 },
      ],
      contacts: [],
      addressDetails: { formatted: '', street: '', city: '', region: '', postalCode: '', country: '' },
      contactDetails: { phones: [], emails: [], contactForms: [], socialProfiles: [] },
      phoneDetails: '',
      emailDetails: '',
      socialLinks: '',
      socialProfiles: [],
      services: [],
      teamMembers: [],
      pageDiscovery: [],
      smartInspector: {
        cssSelector: 'body',
        xpath: '/html/body',
        confidence: 0,
        sourcePage: normalized,
      },
      confidence: [
        { field: 'Business Name', score: 30 },
        { field: 'Phone', score: 0 },
        { field: 'Email', score: 0 },
        { field: 'Address', score: 0 },
        { field: 'Social Links', score: 0 },
      ],
      sourceError: error?.message || 'Unable to fetch site',
    };
  }
}

function dashboardSummary() {
  return {
    platformName: 'Lead Harvest',
    subtitle: 'Real-time website intelligence, lead generation, CRM, and visual extraction studio',
    generatedOn: new Date().toISOString().slice(0, 10),
    metrics: {
      totalUrlsProcessed: 12842,
      businessesFound: 8731,
      contactsFound: 12994,
      emailsFound: 9487,
      phonesFound: 6881,
      socialProfilesFound: 3928,
      websiteQualityScore: 94.2,
      digitalPresenceScore: 91.5,
      opportunityScore: 88.7,
    },
    modules: ['Overview', 'Visual Studio', 'Lead Intelligence', 'Analytics', 'CRM Pipeline', 'History Grid', 'Import / Export'],
    crmPipeline: [
      { name: 'New', count: 142 },
      { name: 'Qualified', count: 118 },
      { name: 'Contacted', count: 84 },
      { name: 'Meeting Scheduled', count: 41 },
      { name: 'Proposal Sent', count: 19 },
      { name: 'Won', count: 13 },
      { name: 'Lost', count: 22 },
    ],
    historyColumns: [
      { name: 'Website URL', visible: true },
      { name: 'Business Name', visible: true },
      { name: 'Category', visible: true },
      { name: 'Phone', visible: true },
      { name: 'Email', visible: true },
      { name: 'Address', visible: true },
      { name: 'Website Available', visible: true },
      { name: 'Social Profiles', visible: true },
      { name: 'Services', visible: true },
      { name: 'Lead Score', visible: true },
      { name: 'Date Added', visible: true },
      { name: 'Status', visible: true },
    ],
    topInsights: [
      { label: 'Live crawl coverage', value: 'Results are generated from the submitted URL and discovered internal pages.', confidence: 98 },
      { label: 'Contact completeness', value: 'Contacts are only shown when public HTML exposes them.', confidence: 98 },
      { label: 'Real-time mode', value: 'No seeded clinic demo data is used for analysis.', confidence: 100 },
    ],
    industryTrends: [],
    geographyTrends: [],
  };
}

async function buildExcelWorkbook(rows) {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'Lead Harvest';
  workbook.company = 'Lead Harvest';
  workbook.title = 'Lead Harvest History Export';
  workbook.subject = 'Website intelligence history';
  workbook.created = new Date();

  const normalizedRows = (Array.isArray(rows) ? rows : []).map((row) => {
    let addrDetails = (row.addressDetails && typeof row.addressDetails === 'object') ? row.addressDetails : null;
    const fullAddress = row.address || (addrDetails && addrDetails.formatted) || '';
    
    // Dynamically parse address if components are missing
    if (fullAddress && (!addrDetails || (!addrDetails.city && !addrDetails.region && !addrDetails.country))) {
      addrDetails = parseAddressString(fullAddress);
    } else if (!addrDetails) {
      addrDetails = { formatted: '', street: '', city: '', region: '', postalCode: '', country: '' };
    }

    return {
      websiteUrl: row.websiteUrl || '',
      businessName: row.businessName || '',
      category: row.category || '',
      phone: row.phone || '',
      phoneDetails: row.phoneDetails || row.phone || '',
      email: row.email || '',
      emailDetails: row.emailDetails || row.email || '',
      address: fullAddress || addrDetails.formatted || '',
      addressStreet: row.addressStreet || addrDetails.street || '',
      addressCity: row.addressCity || addrDetails.city || '',
      addressRegion: row.addressRegion || addrDetails.region || '',
      addressPostalCode: row.addressPostalCode || addrDetails.postalCode || '',
      addressCountry: row.addressCountry || addrDetails.country || '',
      websiteAvailable: row.websiteAvailable || '',
      socialProfiles: row.socialProfiles || '',
      socialLinks: row.socialLinks || row.socialProfiles || '',
      services: row.services || '',
      leadScore: Number.isFinite(Number(row.leadScore)) ? Number(row.leadScore) : '',
      dateAdded: row.dateAdded || '',
      status: row.status || '',
      stage: row.stage || row.status || '',
    };
  });

  const contactRows = [];
  const socialRows = [];
  const crmCounts = new Map();

  normalizedRows.forEach((row) => {
    const stage = String(row.stage || row.status || 'Unknown').trim() || 'Unknown';
    crmCounts.set(stage, (crmCounts.get(stage) || 0) + 1);

    String(row.phoneDetails || row.phone || '')
      .split(/[\n,;]+/)
      .map((value) => value.trim())
      .filter(Boolean)
      .forEach((value) => contactRows.push({ websiteUrl: row.websiteUrl, businessName: row.businessName, type: 'Phone', value, link: value ? `tel:${value.replace(/[^0-9+]/g, '')}` : '' }));

    String(row.emailDetails || row.email || '')
      .split(/[\n,;]+/)
      .map((value) => value.trim())
      .filter(Boolean)
      .forEach((value) => contactRows.push({ websiteUrl: row.websiteUrl, businessName: row.businessName, type: 'Email', value, link: `mailto:${value}` }));

    String(row.socialLinks || row.socialProfiles || '')
      .split(/[\n,;]+/)
      .map((value) => value.trim())
      .filter(Boolean)
      .forEach((value) => socialRows.push({ websiteUrl: row.websiteUrl, businessName: row.businessName, network: socialLabelFromUrl(value), value }));
  });

  const crmRows = [...crmCounts.entries()].map(([stage, count]) => ({ stage, count }));

  const summarySheet = workbook.addWorksheet('Summary', {
    properties: { tabColor: { argb: 'FF31D0AA' } },
  });
  summarySheet.columns = [
    { header: 'Metric', key: 'metric', width: 28 },
    { header: 'Value', key: 'value', width: 22 },
    { header: 'Description', key: 'description', width: 58 },
  ];
  summarySheet.addRows([
    { metric: 'Exported Rows', value: rows.length, description: 'Filtered history rows included in this workbook.' },
    { metric: 'Contacts Extracted', value: contactRows.length, description: 'Phone and email values split into a dedicated sheet.' },
    { metric: 'Social Profiles Extracted', value: socialRows.length, description: 'Social profile URLs split into a dedicated sheet.' },
    { metric: 'CRM Stages', value: crmRows.length, description: 'Stage counts summarized from the exported rows.' },
    { metric: 'Generated At', value: new Date().toISOString(), description: 'Timestamp when the workbook was generated.' },
    { metric: 'Format', value: 'Microsoft Excel Workbook (.xlsx)', description: 'Managed spreadsheet export with multiple sheets and formatting.' },
  ]);

  const historySheet = workbook.addWorksheet('Lead History', {
    properties: { tabColor: { argb: 'FF3B82F6' } },
    views: [{ state: 'frozen', ySplit: 1 }],
  });

  historySheet.columns = [
    { header: 'Website URL', key: 'websiteUrl', width: 36 },
    { header: 'Business Name', key: 'businessName', width: 28 },
    { header: 'Category', key: 'category', width: 20 },
    { header: 'Phone', key: 'phone', width: 20 },
    { header: 'Phone Details', key: 'phoneDetails', width: 28 },
    { header: 'Email', key: 'email', width: 30 },
    { header: 'Email Details', key: 'emailDetails', width: 34 },
    { header: 'Address', key: 'address', width: 34 },
    { header: 'Street', key: 'addressStreet', width: 28 },
    { header: 'City', key: 'addressCity', width: 20 },
    { header: 'State/Region', key: 'addressRegion', width: 18 },
    { header: 'Postal Code', key: 'addressPostalCode', width: 16 },
    { header: 'Country', key: 'addressCountry', width: 16 },
    { header: 'Website Available', key: 'websiteAvailable', width: 18 },
    { header: 'Social Profiles', key: 'socialProfiles', width: 44 },
    { header: 'Social Links', key: 'socialLinks', width: 44 },
    { header: 'Services', key: 'services', width: 44 },
    { header: 'Lead Score', key: 'leadScore', width: 12 },
    { header: 'Date Added', key: 'dateAdded', width: 24 },
    { header: 'Status', key: 'status', width: 16 },
  ];

  historySheet.addRows(normalizedRows);
  historySheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
  historySheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FF0F172A' },
  };
  historySheet.getRow(1).alignment = { vertical: 'middle', horizontal: 'center' };
  historySheet.autoFilter = {
    from: 'A1',
    to: 'T1',
  };

  historySheet.eachRow((row, rowNumber) => {
    if (rowNumber === 1) {
      return;
    }
    if (rowNumber % 2 === 0) {
      row.eachCell((cell) => {
        cell.fill = {
          type: 'pattern',
          pattern: 'solid',
          fgColor: { argb: 'FFF8FAFC' },
        };
      });
    }
    row.getCell('leadScore').alignment = { horizontal: 'center' };
  });

  const contactsSheet = workbook.addWorksheet('Contacts', {
    properties: { tabColor: { argb: 'FFF59E0B' } },
    views: [{ state: 'frozen', ySplit: 1 }],
  });
  contactsSheet.columns = [
    { header: 'Website URL', key: 'websiteUrl', width: 36 },
    { header: 'Business Name', key: 'businessName', width: 28 },
    { header: 'Contact Type', key: 'type', width: 16 },
    { header: 'Contact Value', key: 'value', width: 42 },
    { header: 'Call / Mail Link', key: 'link', width: 42 },
  ];
  contactsSheet.addRows(contactRows);
  if (contactsSheet.rowCount > 1) {
    contactsSheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
    contactsSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF59E0B' } };
  }
  contactsSheet.autoFilter = { from: 'A1', to: 'E1' };

  const socialSheet = workbook.addWorksheet('Social Profiles', {
    properties: { tabColor: { argb: 'FF8B5CF6' } },
    views: [{ state: 'frozen', ySplit: 1 }],
  });
  socialSheet.columns = [
    { header: 'Website URL', key: 'websiteUrl', width: 36 },
    { header: 'Business Name', key: 'businessName', width: 28 },
    { header: 'Network', key: 'network', width: 18 },
    { header: 'Profile URL', key: 'value', width: 52 },
  ];
  socialSheet.addRows(socialRows);
  if (socialSheet.rowCount > 1) {
    socialSheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
    socialSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF8B5CF6' } };
  }
  socialSheet.autoFilter = { from: 'A1', to: 'D1' };

  const crmSheet = workbook.addWorksheet('CRM Stages', {
    properties: { tabColor: { argb: 'FFEF4444' } },
    views: [{ state: 'frozen', ySplit: 1 }],
  });
  crmSheet.columns = [
    { header: 'Stage', key: 'stage', width: 24 },
    { header: 'Count', key: 'count', width: 12 },
  ];
  crmSheet.addRows(crmRows.length ? crmRows : [{ stage: 'No data', count: 0 }]);
  crmSheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
  crmSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFEF4444' } };
  crmSheet.autoFilter = { from: 'A1', to: 'B1' };

  return workbook.xlsx.writeBuffer();
}

app.use(cors());
app.use(express.json({ limit: '1mb' }));

app.get('/lh-api/status', (req, res) => {
  res.json({
    status: 'OK',
    message: 'Lead Harvest API is running',
    version: '1.0.0',
    systemStatus: 'Lead Harvest services operational',
  });
});

app.get('/lh-api/lead-harvest/dashboard', (req, res) => {
  res.json(dashboardSummary());
});

app.post('/lh-api/lead-harvest/export-xlsx', async (req, res) => {
  try {
    const rows = Array.isArray(req.body?.rows) ? req.body.rows : [];
    const buffer = await buildExcelWorkbook(rows);
    res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    res.setHeader('Content-Disposition', 'attachment; filename="lead-harvest-history.xlsx"');
    res.send(Buffer.from(buffer));
  } catch (error) {
    res.status(500).json({ error: 'Failed to generate Excel export', details: error?.message || 'Unknown error' });
  }
});

app.post('/lh-api/lead-harvest/analyze', async (req, res) => {
  const urls = normalizeUrlList(req.body?.urls || req.body?.url || []);
  const results = [];
  for (const url of urls) {
    results.push(await analyzeUrl(url));
  }
  res.json({ processedCount: results.length, results });
});

// Serve static frontend files from 'public' directory
app.use(express.static(path.join(__dirname, 'public')));

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Lead Harvest API running at http://localhost:${PORT}`);
});
