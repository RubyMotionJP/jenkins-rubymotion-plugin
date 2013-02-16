require "tempfile"

class RubymotionBuilder < Jenkins::Tasks::Builder

    display_name "RubyMotion"

    attr_accessor :device_family_type
    attr_accessor :retina_mode
    attr_accessor :output_style_type
    attr_accessor :simulator_version
    attr_accessor :use_bundler
    attr_accessor :need_clean

    # Invoked with the form parameters when this extension point
    # is created from a configuration screen.
    def initialize(attrs = {})
      @rake_task_type     = attrs['rake_task_type']
      @output_style_type  = attrs['output_style_type']
      @output_file_name   = attrs['output_file_name']
      @device_family_type = attrs['device_family_type']
      @simulator_version  = attrs['simulator_version']
      @retina_mode        = attrs['retina_mode']
      @use_bundler        = attrs['use_bundler']
      @need_clean         = attrs['need_clean']
    end

    ##
    # Runs before the build begins
    #
    # @param [Jenkins::Model::Build] build the build which will begin
    # @param [Jenkins::Model::Listener] listener the listener for this build.
    def prebuild(build, listener)
      # do any setup that needs to be done before this build runs.
      @workspace = build.workspace.realpath
      @output_file_path = "#{@workspace}/#{@output_file_name}"
      if File.exist?(@output_file_path)
        File.delete(@output_file_path)
      end
    end

    ##
    # Runs the step over the given build and reports the progress to the listener.
    #
    # @param [Jenkins::Model::Build] build on which to run this step
    # @param [Jenkins::Launcher] launcher the launcher that can run code on the node running this build
    # @param [Jenkins::Model::Listener] listener the listener for this build.
    def perform(build, launcher, listener)
      # actually perform the build step
      env = build.native.getEnvironment()
      path = env['PATH+RBENV'] || env['PATH+RVM'] || env['PATH'] || ""
      path.sub!(/:+$/, '')
      path = path + ":" if path.length > 0
      path = path + "/usr/bin:/bin"
      lang = env['LANG']

      if @need_clean
        rake = "rake clean"
        rake = "bundle exec #{rake}" if @use_bundler
        launcher.execute("bash", "-c", "export LANG=#{lang}; export PATH=#{path}; #{rake}", {:chdir => @workspace, :out => listener})
      end

      rake = "rake #{@rake_task_type}"
      rake = "bundle exec #{rake}" if @use_bundler

      rake << " target=#{@simulator_version}" if @simulator_version.to_s.length > 0
      rake << " device_family=#{@device_family_type}"
      rake << " retina=#{@retina_mode}"
      rake << " output=#{@output_style_type}"

      stderr_file = Tempfile.new("stderr")
      rake << " SIM_STDOUT_PATH=#{@output_file_path} SIM_STDERR_PATH=#{stderr_file.path}"

      launcher.execute("bash", "-c", "export LANG=#{lang}; export PATH=#{path}; #{rake}", {:chdir => @workspace, :out => listener})
      stderr_file.close

      listener << File.read(@output_file_path)
    end

end