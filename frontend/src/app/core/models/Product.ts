export interface Product {
  id: string;       
  name: string;
  image: string;
  description: string;
  price: number;
  userId: string;     
  category?: string;
  artisan?: string;
  region?: string;
  createdAt?: string; 
  updatedAt?: string; 
}

export const products: Product[] = [
  {
    id: '1',
    name: 'Hand-Woven Beni Ourain Carpet',
    description: 'Traditional Berber wool carpet from the Middle Atlas mountains, featuring authentic geometric patterns.',
    price: 4500,
    userId: 'user1',
    category: 'Textiles',
    artisan: 'Fatima Zohra',
    region: 'Middle Atlas',
    image: 'https://images.unsplash.com/photo-1576016773942-31757724f791?auto=format&fit=crop&w=800&q=80',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '2',
    name: 'Fez Blue Ceramic Vase',
    description: 'Exquisite hand-painted ceramic vase using traditional Fez blue cobalt glaze and intricate patterns.',
    price: 850,
    userId: 'user2',
    category: 'Ceramics',
    artisan: 'Ahmed El Fassi',
    region: 'Fez',
    image: 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?auto=format&fit=crop&w=800&q=80',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '3',
    name: 'Pure Culinary Argan Oil',
    description: 'Organic, cold-pressed culinary argan oil from the Essaouira region. Rich in antioxidants and nutty flavor.',
    price: 320,
    userId: 'user3',
    category: 'Food',
    artisan: 'Cooperative Amal',
    region: 'Essaouira',
    image: 'https://images.unsplash.com/photo-1608500218890-c4f923e38707?auto=format&fit=crop&w=800&q=80',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '4',
    name: 'Embroidered Silk Kaftan',
    description: 'Luxurious handmade silk kaftan with traditional "Randa" embroidery and gold thread details.',
    price: 2800,
    userId: 'user4',
    category: 'Clothing',
    artisan: 'Lalla Salma',
    region: 'Marrakech',
    image: 'https://images.unsplash.com/photo-1605342082260-26466f272a27?auto=format&fit=crop&w=800&q=80',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '5',
    name: 'Hand-Carved Cedar Wood Box',
    description: 'Fragrant cedar wood box from the Atlas mountains, hand-carved with traditional geometric motifs.',
    price: 450,
    userId: 'user5',
    category: 'Woodwork',
    artisan: 'Youssef Alami',
    region: 'Ifrane',
    image: 'https://images.unsplash.com/photo-1590650516494-0c8e4a4dd67e?auto=format&fit=crop&w=800&q=80',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '6',
    name: 'Taza Leather Satchel',
    description: 'Supple, naturally tanned leather satchel made by traditional techniques in the Taza region.',
    price: 1200,
    userId: 'user6',
    category: 'Leather',
    artisan: 'Mohammed Tazi',
    region: 'Taza',
    image: 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=800&q=80',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
];


