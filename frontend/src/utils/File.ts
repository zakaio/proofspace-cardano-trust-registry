export const readFile = async (file: File): Promise<any> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onabort = () => {
      console.log('file reading was aborted');
      reject(new Error('File reading aborted'));
    };
    reader.onerror = () => {
      console.log('file reading has failed');
      reject(new Error('File reading error'));
    };
    reader.onload = () => {
      resolve(reader.result);
    };
    reader.readAsDataURL(file);
  });
};
