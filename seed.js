const fs = require('fs');
const path = require('path');
const https = require('https');

// 1. Parse Project Configurations dynamically from Android's google-services.json
const googleServicesPath = path.join(__dirname, 'app', 'google-services.json');
let projectId = 'bajuadat-app';
let apiKey = '';

if (fs.existsSync(googleServicesPath)) {
  try {
    const data = JSON.parse(fs.readFileSync(googleServicesPath, 'utf8'));
    projectId = data.project_info.project_id;
    apiKey = data.client[0].api_key[0].current_key;
    console.log(`\x1b[32m✔ Menemukan google-services.json untuk Project ID: "${projectId}"\x1b[0m`);
  } catch (err) {
    console.error('\x1b[31m⚠ Gagal menguraikan google-services.json, menggunakan konfigurasi cadangan.\x1b[0m', err.message);
  }
} else {
  console.log('\x1b[33m⚠ File google-services.json tidak terdeteksi di /app, menggunakan fallback default.\x1b[0m');
}

// 2. Helper function to make REST requests to Firebase Firestore
function firestoreRequest(method, documentPath, bodyData = null) {
  return new Promise((resolve, reject) => {
    const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${documentPath}?key=${apiKey}`;
    const payload = bodyData ? JSON.stringify(bodyData) : null;
    
    const parsedUrl = new URL(url);
    const options = {
      hostname: parsedUrl.hostname,
      port: 443,
      path: parsedUrl.pathname + parsedUrl.search,
      method: method,
      headers: {
        'Content-Type': 'application/json'
      }
    };
    
    if (payload) {
      options.headers['Content-Length'] = Buffer.byteLength(payload);
    }
    
    const req = https.request(options, (res) => {
      let responseBody = '';
      res.on('data', (chunk) => { responseBody += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(responseBody);
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(json);
          } else {
            reject(new Error(`Code ${res.statusCode}: ${json.error ? json.error.message : responseBody}`));
          }
        } catch (e) {
          reject(new Error(`Gagal mengurai respons: ${responseBody}`));
        }
      });
    });
    
    req.on('error', (err) => {
      reject(err);
    });
    
    if (payload) {
      req.write(payload);
    }
    req.end();
  });
}

// 3. Convert Javascript values/objects into the exact structured format Firestore REST API expects
function valueToFirestore(value) {
  if (value === null || value === undefined) {
    return { nullValue: null };
  }
  if (typeof value === 'boolean') {
    return { booleanValue: value };
  }
  if (typeof value === 'number') {
    if (Number.isInteger(value)) {
      return { integerValue: String(value) };
    }
    return { doubleValue: value };
  }
  if (typeof value === 'string') {
    return { stringValue: value };
  }
  if (Array.isArray(value)) {
    return {
      arrayValue: {
        values: value.map(v => valueToFirestore(v))
      }
    };
  }
  if (typeof value === 'object') {
    const fields = {};
    for (const [key, val] of Object.entries(value)) {
      fields[key] = valueToFirestore(val);
    }
    return { mapValue: { fields } };
  }
  return { stringValue: String(value) };
}

function objectToFirestoreFields(obj) {
  const fields = {};
  for (const [key, val] of Object.entries(obj)) {
    fields[key] = valueToFirestore(val);
  }
  return { fields };
}

// 4. Main script execution
async function runSeeding() {
  console.log('\n======================================================');
  console.log('       🔥 DATABASE SEEDING FOR FIRESTORE 🔥           ');
  console.log('======================================================\n');
  
  if (!apiKey) {
    console.error('\x1b[31m❌ API Key kosong. Pastikan apk-key di config/google-services.json valid!\x1b[0m');
    process.exit(1);
  }

  // --- A. Query Dynamic UID from existing users collection ---
  console.log('🔍 Mencari akun pengguna terdaftar dari koleksi "users"...');
  let targetUid = process.argv[2] || 'dummy_user_uid_178201201'; // Default Fallback UID
  
  try {
    const listUsers = await firestoreRequest('GET', 'users');
    if (listUsers && listUsers.documents && listUsers.documents.length > 0) {
      // Get the document name e.g., "projects/bajuadat-app/databases/(default)/documents/users/UID"
      const firstDocName = listUsers.documents[0].name;
      const parsedUid = firstDocName.split('/').pop();
      if (parsedUid) {
        targetUid = parsedUid;
        console.log(`\x1b[32m✔ Menemukan UID Pengguna Asli: "${targetUid}"\x1b[0m`);
      }
    } else {
      console.log(`\x1b[33mℹ Koleksi "users" masih kosong. Menggunakan UID default: "${targetUid}"\x1b[0m`);
    }
  } catch (err) {
    console.log(`\x1b[33m⚠ Tidak bisa membaca koleksi "users" (kemungkinan aturan Firestore membatasi atau belum ada data). Menggunakan UID: "${targetUid}"\x1b[0m`);
  }

  // --- B. Seeding products ---
  const productsToSeed = [
    {
      id: 1,
      product_id: 1,
      name: "Baju Safari Pria Putih Agung",
      description: "Kemeja Safari Adat Bali lengan pendek berwarna putih bersih berkualitas tinggi dengan bahan katun premium. Nyaman, menyerap keringat, dan didesain pas untuk upacara adat.",
      price: 185000.0,
      base_price: 185000.0,
      category: "Pria",
      imageUrl: "pria_safari",
      stock: 15,
      is_active: true
    },
    {
      id: 2,
      product_id: 2,
      name: "Kamen Songket Bali Pria Premium",
      description: "Sarung Kamen bermotif songket khas Bali dengan tenunan benang emas yang mewah dan gagah untuk upacara adat formal.",
      price: 275000.0,
      base_price: 275000.0,
      category: "Pria",
      imageUrl: "pria_kamen_songket",
      stock: 12,
      is_active: true
    },
    {
      id: 3,
      product_id: 3,
      name: "Udeng Dewata Putih Polos",
      description: "Ikat kepala tradisional khas Bali (Udeng) warna putih bersih dengan lipatan simetris melambangkan kejernihan pikiran.",
      price: 45000.0,
      base_price: 45000.0,
      category: "Pria",
      imageUrl: "pria_udeng",
      stock: 25,
      is_active: true
    },
    {
      id: 4,
      product_id: 4,
      name: "Saput Poleng Bali Klasik",
      description: "Kain luar (Saput) dengan corak kotak-kotak hitam putih khas Bali yang melambangkan keharmonisan teologi Rua Bhineda.",
      price: 95000.0,
      base_price: 95000.0,
      category: "Pria",
      imageUrl: "pria_saput",
      stock: 20,
      is_active: true
    },
    {
      id: 5,
      product_id: 5,
      name: "Kebaya Bali Brokat Kuning Kunyit",
      description: "Kebaya adat wanita Bali berbahan brokat elastis yang halus dan nyaman dipakai mengikuti postur tubuh dengan motif anggun.",
      price: 195000.0,
      base_price: 195000.0,
      category: "Wanita",
      imageUrl: "wanita_kebaya",
      stock: 10,
      is_active: true
    },
    {
      id: 6,
      product_id: 6,
      name: "Kamen Prada Wanita Merah Mas",
      description: "Kain kamen bawahan wanita bermotif tinta prada emas bercahaya di atas katun premium yang lembut.",
      price: 165000.0,
      base_price: 165000.0,
      category: "Wanita",
      imageUrl: "wanita_kamen_prada",
      stock: 14,
      is_active: true
    },
    {
      id: 7,
      product_id: 7,
      name: "Selendang Bali (Anteng) Cerah",
      description: "Selendang pinggang sutra tipis mengkilap berhias renda indah di kedua ujungnya untuk upacara persembahyangan.",
      price: 35000.0,
      base_price: 35000.0,
      category: "Wanita",
      imageUrl: "wanita_selendang",
      stock: 30,
      is_active: true
    },
    {
      id: 8,
      product_id: 8,
      name: "Sanggul Bali Set & Jepun",
      description: "Set sanggul pusung tagel adat Bali instan lengkap dengan kelopak bunga jepun kamboja segar tiruan bermutu tinggi.",
      price: 85000.0,
      base_price: 85000.0,
      category: "Wanita",
      imageUrl: "wanita_sanggul",
      stock: 15,
      is_active: true
    },
    {
      id: 9,
      product_id: 9,
      name: "Udeng Putih Anak Dewata",
      description: "Ikat kepala Udeng Bali putih bersih bermotif emas manis berukuran lingkar kepala anak-anak, nyaman dipakai.",
      price: 35000.0,
      base_price: 35000.0,
      category: "Aksesoris",
      imageUrl: "pria_udeng",
      stock: 18,
      is_active: true
    },
    {
      id: 10,
      product_id: 10,
      name: "Kebaya Bali Laksmi Moderen",
      description: "Kebaya modern berkerah sabrina elegan dengan detail bordir bunga lili kontemporer yang modis untuk generasi muda mas.",
      price: 215000.0,
      base_price: 215000.0,
      category: "Wanita",
      imageUrl: "wanita_kebaya",
      stock: 8,
      is_active: true
    }
  ];

  console.log(`\n📦 Menyemai ${productsToSeed.length} produk adat ke koleksi "products"...`);
  for (const prod of productsToSeed) {
    try {
      const documentPath = `products/${prod.id}`;
      const payload = objectToFirestoreFields(prod);
      await firestoreRequest('PATCH', documentPath, payload);
      console.log(`   └─ Selesai: "${prod.name}"`);
    } catch (err) {
      console.error(`   ❌ Gagal: "${prod.name}": ${err.message}`);
    }
  }

  // --- C. Seeding dummy carts ---
  console.log(`\n🛒 Menyemai 1 item keranjang belanja dummy terikat UID: "${targetUid}"...`);
  try {
    const cartData = {
      userId: targetUid,
      productId: 3, // Udeng Dewata
      quantity: 2
    };
    // Format document ID match mobile: "${userId}_${productId}"
    const cartDocId = `${targetUid}_3`;
    const payload = objectToFirestoreFields(cartData);
    await firestoreRequest('PATCH', `carts/${cartDocId}`, payload);
    console.log(`   └─ Selesai: Membuat item keranjang belanja Udeng Dewata untuk UID: ${targetUid}`);
  } catch (err) {
    console.error(`   ❌ Gagal menyemai Keranjang Belanja: ${err.message}`);
  }

  // --- D. Seeding dummy orders (historical transactions) ---
  console.log(`\n🧾 Menyemai 2 riwayat transaksi dummy ke koleksi "orders"...`);
  
  const order1 = {
    userId: targetUid,
    totalPrice: 220000.0,
    date: "10 Jun 2026, 11:20",
    timestamp: Date.now() - (24 * 60 * 60 * 1000), // Kemarin
    status: "PAID", // Sesuai permintaan: PAID
    items: [
      { name: "Baju Safari Pria Putih Agung", qty: 1, price: 185000.0 },
      { name: "Selendang Bali (Anteng) Cerah", qty: 1, price: 35000.0 }
    ],
    shippingAddress: "Jl. Raya Ubud No. 12, Ubud, Gianyar, Bali",
    latitude: -8.5069,
    longitude: 115.2625,
    eta: "Tiba Hari Ini",
    aiDispatchReport: "Selesai dianalisis oleh Gemini Flash-Lite. Pengemasan selesai dan kurir Go-Send Adat Bali sedang dalam perjalanan menuju lokasi Anda."
  };

  const order2 = {
    userId: targetUid,
    totalPrice: 260000.0,
    date: "09 Jun 2026, 09:40",
    timestamp: Date.now() - (2 * 24 * 60 * 60 * 1000), // 2 hari lalu
    status: "DELIVERED", // Sesuai permintaan: DELIVERED
    items: [
      { name: "Kebaya Bali Brokat Kuning Kunyit", qty: 1, price: 195000.0 },
      { name: "Kamen Prada Wanita Merah Mas", qty: 1, price: 165000.0 }
    ],
    shippingAddress: "Hotel Maya, Ubud, Gianyar, Bali",
    latitude: -8.5134,
    longitude: 115.2711,
    eta: "Sudah Diterima",
    aiDispatchReport: "Rute Gianyar Hub -> Hotel Maya diselesaikan dalam waktu 18 menit. Pesanan telah diterima dengan aman oleh resepsionis hotel."
  };

  try {
    const payload1 = objectToFirestoreFields(order1);
    await firestoreRequest('PATCH', `orders/dummy_order_paid`, payload1);
    console.log(`   └─ Selesai: Membuat Transaksi PAID (ID: dummy_order_paid)`);
    
    const payload2 = objectToFirestoreFields(order2);
    await firestoreRequest('PATCH', `orders/dummy_order_delivered`, payload2);
    console.log(`   └─ Selesai: Membuat Transaksi DELIVERED (ID: dummy_order_delivered)`);
  } catch (err) {
    console.error(`   ❌ Gagal menyemai Transaksi Orders: ${err.message}`);
  }

  console.log('\n======================================================');
  console.log('   🎉 DATABASE SEEDING BEKERJA DENGAN SUKSES! 🎉      ');
  console.log('======================================================\n');
}

runSeeding();
