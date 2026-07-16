const axios = require('axios');

async function testMassExtract() {
  console.log('=== Testing Mass Extract Real API Calls ===\n');

  // Step 1: Geocode Bhopal
  console.log('1. Geocoding "Bhopal"...');
  const geoResp = await axios.get(
    'https://nominatim.openstreetmap.org/search?q=Bhopal&format=json&limit=1',
    { headers: { 'User-Agent': 'LeadHarvest/1.0' }, timeout: 10000 }
  );
  
  if (!geoResp.data?.length) {
    console.error('FAILED: Could not geocode Bhopal');
    return;
  }
  
  const lat = geoResp.data[0].lat;
  const lon = geoResp.data[0].lon;
  console.log(`   ✓ Bhopal => lat: ${lat}, lng: ${lon}\n`);

  // Step 2: Search cafes via Overpass API
  console.log('2. Searching for "cafe" near Bhopal (10km radius)...');
  const overpassQuery = `
    [out:json][timeout:30];
    (
      node["amenity"="cafe"](around:10000,${lat},${lon});
      way["amenity"="cafe"](around:10000,${lat},${lon});
    );
    out center body 20;
  `;

  const overpassResp = await axios.post(
    'https://overpass-api.de/api/interpreter',
    `data=${encodeURIComponent(overpassQuery)}`,
    { timeout: 30000, headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'LeadHarvest/1.0' } }
  );

  const elements = overpassResp.data.elements || [];
  console.log(`   ✓ Found ${elements.length} raw results\n`);

  // Step 3: Display results
  console.log('3. Real Business Data:\n');
  elements.forEach((el, i) => {
    const t = el.tags || {};
    const name = t.name || t['name:en'] || '(unnamed)';
    if (!t.name && !t['name:en']) return;
    
    console.log(`   [${i + 1}] ${name}`);
    console.log(`       Category: cafe`);
    console.log(`       Phone: ${t.phone || t['contact:phone'] || 'N/A'}`);
    console.log(`       Email: ${t.email || t['contact:email'] || 'N/A'}`);
    console.log(`       Website: ${t.website || t['contact:website'] || 'N/A'}`);
    console.log(`       Address: ${[t['addr:housenumber'], t['addr:street'], t['addr:city'], t['addr:postcode']].filter(Boolean).join(', ') || 'N/A'}`);
    console.log(`       Opening Hours: ${t.opening_hours || 'N/A'}`);
    console.log(`       OSM Link: https://www.openstreetmap.org/${el.type}/${el.id}`);
    console.log(`       Coords: ${el.lat || el.center?.lat}, ${el.lon || el.center?.lon}`);
    console.log('');
  });

  // Step 4: Test restaurant search too
  console.log('\n4. Searching for "restaurant" near Bhopal...');
  const restQuery = `
    [out:json][timeout:30];
    (
      node["amenity"="restaurant"](around:10000,${lat},${lon});
      way["amenity"="restaurant"](around:10000,${lat},${lon});
    );
    out center body 10;
  `;

  const restResp = await axios.post(
    'https://overpass-api.de/api/interpreter',
    `data=${encodeURIComponent(restQuery)}`,
    { timeout: 30000, headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'LeadHarvest/1.0' } }
  );

  const restElements = restResp.data.elements || [];
  console.log(`   ✓ Found ${restElements.length} restaurants\n`);
  restElements.forEach((el, i) => {
    const t = el.tags || {};
    if (!t.name) return;
    console.log(`   [${i + 1}] ${t.name} | ${t.cuisine || 'N/A'} | Phone: ${t.phone || 'N/A'} | ${t.website || 'N/A'}`);
  });

  console.log('\n=== Test Complete ===');
}

testMassExtract().catch(e => console.error('Test failed:', e.message));
