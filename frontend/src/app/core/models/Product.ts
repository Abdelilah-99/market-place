export interface Product {
  id: string;       
  name: string;
  image: string;
  images?: string[];
  description: string;
  category?: string;
  price: number;
  userId: string;     
  createdAt?: string; 
  updatedAt?: string; 
}



export const products: Product[] = [
  {
    id: '1',
    name: 'Laptop',
    description: 'High performance laptop',
    price: 1200,
    userId: 'user1',
    image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '2',
    name: 'Smartphone',
    description: 'Latest smartphone with OLED screen',
    price: 900,
    userId: 'user2',
    image: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '3',
    name: 'Headphones',
    description: 'Noise-cancelling headphones',
    price: 250,
    userId: 'user3',
    image: 'https://images.unsplash.com/photo-1511367461989-f85a21fda167?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '4',
    name: 'Keyboard',
    description: 'Mechanical keyboard with RGB lights',
    price: 120,
    userId: 'user4',
    image: 'https://images.unsplash.com/photo-1585079549942-bff0e4c7f61d?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '5',
    name: 'Mouse',
    description: 'Wireless gaming mouse',
    price: 80,
    userId: 'user5',
    image: 'https://images.unsplash.com/photo-1580927752452-3f0e81c62406?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '6',
    name: 'Monitor',
    description: '27-inch 4K monitor',
    price: 400,
    userId: 'user6',
    image: 'https://images.unsplash.com/photo-1553531384-6b5a7f85072b?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '7',
    name: 'Tablet',
    description: '10-inch tablet with stylus support',
    price: 600,
    userId: 'user7',
    image: 'https://images.unsplash.com/photo-1580910051071-4502e13b85b8?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '8',
    name: 'Smartwatch',
    description: 'Fitness smartwatch with heart rate monitor',
    price: 200,
    userId: 'user8',
    image: 'https://images.unsplash.com/photo-1603791440384-56cd371ee9a7?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '9',
    name: 'Camera',
    description: 'DSLR camera for photography',
    price: 1500,
    userId: 'user9',
    image: 'https://images.unsplash.com/photo-1508898578281-774ac4893a2f?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: '10',
    name: 'Speaker',
    description: 'Bluetooth portable speaker',
    price: 100,
    userId: 'user10',
    image: 'https://images.unsplash.com/photo-1610246908866-2d6b9dcfb1de?auto=format&fit=crop&w=500&q=60',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
];
