"use strict";

const app = angular.module('noScalpAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('noScalpAppController', function($http, $location, $uibModal) {
    const noScalpApp = this;

    const apiBaseURL = "/api/noScalping/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => noScalpApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    noScalpApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'noScalpAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                noScalpApp: () => noScalpApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    noScalpApp.getDISTRIBUTIONs = () => $http.get(apiBaseURL + "distributions")
        .then((response) => noScalpApp.distributions = Object.keys(response.data)
            .map((key) => response.data[key])
            .reverse());

    noScalpApp.getDISTRIBUTIONs();
});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, noScalpApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

        // Validates and sends DISTRIBUTION.
        modalInstance.create = function validateAndSendDISTRIBUTION() {
            if (modalInstance.form.tickets <= 0) {
                modalInstance.formError = true;
            } else {
                modalInstance.formError = false;
                $uibModalInstance.close();

                let CREATE_DISTRIBUTIONS_PATH = apiBaseURL + "create-distribution"

                let createDISTRIBUTIONData = $.param({
                    distributorName: modalInstance.form.toDistributor,
                    ticketQuantity: modalInstance.form.tickets,
                    eventName: modalInstance.form.showname

                });

                let createDISTRIBUTIONHeaders = {
                    headers : {
                        "Content-Type": "application/x-www-form-urlencoded"
                    }
                };

                // Creates DISTRIBUTION and handles success / fail responses.
                $http.post(CREATE_DISTRIBUTIONS_PATH, createDISTRIBUTIONData, createDISTRIBUTIONHeaders).then(
                    modalInstance.displayMessage,
                    modalInstance.displayMessage
                );
            }
        };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create DISTRIBUTION modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the DISTRIBUTION.
    function invalidFormInput() {
        return isNaN(modalInstance.form.tickets) || (modalInstance.form.toDistributor === undefined) || (modalInstance.form.showname === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});
