angular.module('broccoli', ['restangular', 'ui.bootstrap'])
    .controller('AppsCtrl', function(Restangular, $uibModal, $scope) {
        var vm = this;
        vm.apps = [];
        vm.templates = []

        vm.openModal = openModal;

        activate();

        function activate() {
          Restangular.all("templates").getList().then(function(templates) {
            templates.forEach(function(template) {
              vm.templates.push(template);
              Restangular.one("templates", template).get().then(function(template){
                template.imageUrl = "/assets/" + template.id + ".svg"
                template.instances = [];
                Restangular.all("instances").getList({ "templateId" : template.id }).then(function(instances) {
                  instances.forEach(function(instance) {
                    template.instances.push(instance);
                  });
                });
                vm.apps.push(template);
              });
            });
          });
        }

        function submitStatus(instance, status) {
          Restangular.all("instances")
            .customPOST('"' + status + '"', instance.id, {}, {})
            .then(function(updatedInstance) {
              for (i in updatedInstance) {
                instance[i] = updatedInstance[i];
              };
            });
        }

        $scope.submitStatus = submitStatus;

        function openModal(templateApp) {
          var modalInstance = $uibModal.open({
            animation: true,
            templateUrl: '/assets/newInstanceModal.html',
            controller: 'NewInstanceCtrl',
            controllerAs: 'instCtrl',
            size: undefined,
            resolve: {
              templateId: function () {
                return templateApp.id;
              },
              parameters: function () {
                return templateApp.parameters;
              }
            }
          });

          modalInstance.result.then(function (paramsToValue) {
            Restangular.all("instances").post({
              templateId: templateApp.id,
              parameters: paramsToValue
            }).then(function(newInstance) {
              templateApp.instances.push(newInstance);
            }, function(error) {
              console.log("There was an error creating");
              console.log(error);
            });
          });
        };
    });
