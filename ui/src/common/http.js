import axios from 'axios';

export const IEX = axios.create({
  baseURL: 'https://cloud.iexapis.com/stable/',
  params: {
    token: process.env.iexPublicKey
  },
});
