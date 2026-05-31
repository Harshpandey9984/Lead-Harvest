const express = require('express');
const path = require('path');
const cors = require('cors');

const app = express();
const PORT = 3000;

function normalizeUrl(value) {
  const trimmed = String(value || '').trim();
  if (!trimmed) {
    return 'https://example.com';
  }
  return /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(trimmed) ? trimmed : `https://${trimmed}`;
}

function classifyCategory(domain) {
  const normalized = domain.toLowerCase();
  if (normalized.includes('dent') || normalized.includes('clinic') || normalized.includes('medical')) return 'Healthcare';
  if (normalized.includes('restaurant') || normalized.includes('cafe') || normalized.includes('food')) return 'Hospitality';
  if (normalized.includes('law') || normalized.includes('legal')) return 'Legal Services';
  if (normalized.includes('shop') || normalized.includes('store') || normalized.includes('commerce')) return 'E-Commerce';
  if (normalized.includes('agency') || normalized.includes('studio') || normalized.includes('marketing')) return 'Agency';
  return 'Business Services';
}

function slug(domain) {
  return String(domain || '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
}

function buildDashboardSummary() {
  return {
    platformName: 'Lead Harvest',
    subtitle: 'Enterprise website intelligence, lead generation, CRM, and visual extraction studio',
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
    modules: [
      'Overview',
      'Visual Studio',
      'Lead Intelligence',
      'Analytics',
      'CRM Pipeline',
      'History Grid',
      'Import / Export',
    ],
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
      { label: 'Hot lead coverage', value: '78% of analyzed domains are targetable', confidence: 92 },
      { label: 'Contact completeness', value: 'Average score above 88', confidence: 91 },
      { label: 'Social presence', value: 'LinkedIn is the dominant profile', confidence: 84 },
    ],
    industryTrends: [
      { label: 'Healthcare', value: 31 },
      { label: 'E-Commerce', value: 22 },
      { label: 'Agency', value: 18 },
      { label: 'Hospitality', value: 15 },
      { label: 'Legal Services', value: 14 },
    ],
    geographyTrends: [
      { label: 'North America', value: 44 },
      { label: 'Europe', value: 23 },
      { label: 'Asia Pacific', value: 18 },
      { label: 'Middle East', value: 9 },
      { label: 'Other', value: 6 },
    ],
  };
}

function analyzeUrl(rawUrl) {
  const url = normalizeUrl(rawUrl);
  const domain = new URL(url).host.replace(/^www\./, '');
  const category = classifyCategory(domain);
  const businessName = domain.replace(/\.[^.]+$/, '').split(/[.-]/).filter(Boolean).map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(' ') + ' Group';
  const scoreSeed = Math.abs(domain.split('').reduce((accumulator, char) => accumulator + char.charCodeAt(0), 0));
  const leadScore = Math.min(99, 65 + (scoreSeed % 30));
  const websiteQualityScore = Math.min(99, 78 + (scoreSeed % 17));
  const digitalPresenceScore = Math.min(99, 70 + (scoreSeed % 20));
  const seoScore = Math.min(99, 68 + (scoreSeed % 23));
  const contactCompletenessScore = Math.min(99, 72 + (scoreSeed % 18));

  return {
    url,
    businessName,
    category,
    leadScore,
    websiteQualityScore,
    digitalPresenceScore,
    seoScore,
    contactCompletenessScore,
    leadTier: leadScore >= 80 ? 'Hot' : leadScore >= 60 ? 'Warm' : 'Cold',
    businessInfo: [
      { label: 'Business Name', value: businessName, confidence: 98 },
      { label: 'Category', value: category, confidence: 91 },
      { label: 'Website', value: url, confidence: 100 },
      { label: 'Business Type', value: 'Company', confidence: 84 },
    ],
    contacts: [
      { label: 'Phone', value: `+1-555-010-${scoreSeed % 1000}`.padEnd(14, '0').slice(0, 14), confidence: 72 },
      { label: 'Email', value: `info@${domain}`, confidence: 86 },
      { label: 'Contact Form', value: `${url.replace(/\/$/, '')}/contact`, confidence: 93 },
    ],
    socialProfiles: [
      { label: 'LinkedIn', value: `https://www.linkedin.com/company/${slug(domain)}`, confidence: 78 },
      { label: 'Instagram', value: `https://www.instagram.com/${slug(domain)}`, confidence: 64 },
      { label: 'Facebook', value: `https://www.facebook.com/${slug(domain)}`, confidence: 67 },
    ],
    services: [
      { label: 'Core Service', value: `${category} consulting`, confidence: 81 },
      { label: 'Premium Package', value: 'Growth plan', confidence: 75 },
      { label: 'Discovery Call', value: 'Book a strategy session', confidence: 89 },
    ],
    teamMembers: [
      { label: 'Founder', value: `${businessName} Team`, confidence: 71 },
      { label: 'CEO', value: `${businessName} Leadership`, confidence: 66 },
      { label: 'Operations', value: 'Customer success', confidence: 62 },
    ],
    pageDiscovery: ['Home', 'About', 'Services', 'Contact', 'Pricing', 'Blog'],
    smartInspector: {
      cssSelector: 'body',
      xpath: '/html/body',
      confidence: 94,
      sourcePage: url,
    },
    confidence: [
      { field: 'Business Name', score: 98 },
      { field: 'Phone', score: 91 },
      { field: 'Email', score: 88 },
      { field: 'Address', score: 84 },
      { field: 'Social Links', score: 90 },
    ],
  };
}

function normalizeUrlList(input) {
  if (Array.isArray(input)) {
    return input.map(normalizeUrl).filter(Boolean);
  }
  if (typeof input === 'string') {
    return input.split(/[\n\r,;\t]+/).map((value) => value.trim()).filter(Boolean).map(normalizeUrl);
  }
  return [];
}

// Middleware
app.use(cors());
app.use(express.json());
app.use('/lh-api', (req, res, next) => {
  if (req.method === 'GET' && req.path === '/status') {
    res.json({
      status: 'OK',
      message: 'Lead Harvest frontend API is running',
      version: '1.0.0',
      systemStatus: 'Lead Harvest services operational',
    });
    return;
  }

  if (req.method === 'GET' && req.path === '/lead-harvest/dashboard') {
    res.json(buildDashboardSummary());
    return;
  }

  if (req.method === 'POST' && req.path === '/lead-harvest/analyze') {
    const urls = normalizeUrlList(req.body?.urls || req.body?.url || []);
    const results = urls.map(analyzeUrl);
    res.json({ processedCount: results.length, results });
    return;
  }

  next();
});
app.use(express.static(path.join(__dirname, 'public')));

// Routes
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Frontend server running at http://localhost:${PORT}`);
});
