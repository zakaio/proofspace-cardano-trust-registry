import axios from 'axios';
import fs from 'fs';

const languages = ['ua', 'de', 'ar', 'ru', 'en', 'jp', 'fr'];

async function translatorFetch(projectName, language) {
  const response = await axios.get(
    `https://stage.proofspace.id/translator/api/get-locale?projectName=${projectName}&language=${language}`
  );

  return response.data;
}

function updateLocales(language) {
  translatorFetch('trust_registry', language)
    .then((result) => {
      fs.writeFileSync(`src/locales/${language}.json`, JSON.stringify(result));
    })
    .catch((error) => console.error('Error while updating locale', error));
}

if (!fs.existsSync('src/locales')) {
  fs.mkdirSync('src/locales');
}

languages.forEach((language) => {
  updateLocales(language);
});
