new Vue({
    el: '#app',
    data: {
        githubUrl: '', 
        githubToken: '',
        buttonText: 'Import',
        errorStatus: false,
        errorMsg: '',
        link: '',
        apiUrl: '',
        apiHeaders: '',
        size: 0
    },
    methods: {
        onImportButtonClick: async function() {

            const startIndex = this.githubUrl.indexOf('github.') + 'github.'.length;
            const endIndex = this.githubUrl.indexOf('/', startIndex);
            const domain = this.githubUrl.substring(0, endIndex);
            const repo = this.githubUrl.substring(endIndex);

            if (this.githubUrl.includes('github.com')) {
                this.apiUrl = `https://api.github.com/repos${repo}`;
            } else {
                this.apiUrl = `${domain}/api/v3/repos${repo}`;
            }

            if (this.githubToken != '') {
                try {
                    const response = await axios.get(this.apiUrl, {
                        headers: {
                            Authorization: `Bearer ${this.githubToken}`
                        }
                    });
                    this.size = response.data.size;
                    console.log(`Repository size of ${this.githubUrl}/${this.githubToken}: ${this.size} KB`);
                    this.errorStatus = false;
                } catch (error) {
                    console.error('Error fetching repository size:', error);
                    this.errorMsg = this.readError(error.toString());
                    console.log(errorMsg);
                    this.errorStatus = true;
                }
            } else {
                try {
                    const response = await axios.get(this.apiUrl);
                    this.size = response.data.size;
                    console.log(`Repository size of ${this.githubUrl}/${this.githubToken}: ${this.size} KB`);
                    this.errorStatus = false;
                } catch (error) {
                    console.error('Error fetching repository size:', error);
                    this.errorMsg = this.readError(error.toString());
                    this.errorStatus = true;
                }
            }

            
        },
        readError: function(error) {
            if (error.includes('401')) {
                return "Error: Authentication Required";
            } else if (error.includes('404')) {
                return "Error: Repository Not Found";
            } else {
                return error;
            }
        }
        // onImportButtonClick: async function() {
        //     console.log(`Running`)
        //     if(await this.checkValid(this.githubUrl, this.githubToken)) { 
        //         this.link = 'https://codeload.github.com/' + this.githubUrl + "/" + this.githubToken + '/zip/HEAD';  
        //         window.location.href = this.link;
        //         this.buttonText = 'Collected'; 
        //     } else {
        //         this.errorStatus = true;
        //     }
        // },
        // checkValid: async function(owner, repo) {
        //     try {
        //         let response = await fetch(`https://api.github.com/repos/${owner}/${repo}`);
        //         return response.status == 200;
        //     } catch (error) {
        //         return false;
        //     }
        // }
    }
});

